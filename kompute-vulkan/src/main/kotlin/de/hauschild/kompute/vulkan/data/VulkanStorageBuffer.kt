package de.hauschild.kompute.vulkan.data

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.exception.KomputeBackendInitializationException
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import de.hauschild.kompute.core.exception.requireConfiguration
import de.hauschild.kompute.vulkan.VulkanBuffer
import de.hauschild.kompute.vulkan.VulkanReadable
import de.hauschild.kompute.vulkan.backend.VulkanAllocatedBuffer
import de.hauschild.kompute.vulkan.backend.VulkanContext
import de.hauschild.kompute.vulkan.backend.VulkanMemoryKind
import de.hauschild.kompute.vulkan.backend.memoryBarrier
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkCommandBuffer
import java.nio.ByteBuffer

/**
 * Wraps a Vulkan shader storage buffer (SSBO) for a [StorageBuffer].
 *
 * The buffer lives in device-local memory. Data is moved between host and device through
 * temporary staging buffers: [bind] uploads the source data, [read] copies the device buffer into
 * a host-visible staging buffer and converts it back into [T]. Both operations block until the GPU
 * has finished the transfer.
 *
 * Unlike the OpenGL backend, the underlying `VkBuffer` is owned by this instance and released in
 * [close].
 *
 * Note: [LongArray] and [DoubleArray] require the matching shader features (`int64`, `float64`).
 *
 * @param T the data type — must be [IntArray], [LongArray], [FloatArray], [DoubleArray], or [ByteArray]
 * @param source the [StorageBuffer] configuration this buffer is based on
 * @param context the [VulkanContext] used for memory allocation and transfer commands
 */
class VulkanStorageBuffer<T : Any>(
    source: StorageBuffer<T>,
    private val context: VulkanContext,
) : VulkanBuffer<StorageBuffer<T>>(source),
OutputCapable<T> by source,
VulkanReadable<T> {
    private var deviceBuffer: VulkanAllocatedBuffer? = null

    /**
     * Returns the raw `VkBuffer` handle, valid after [bind] and until [close].
     *
     * @return the `VkBuffer` handle (non-dispatchable, hence a plain [Long])
     * @throws KomputeBackendInitializationException if [bind] was not called before
     */
    fun handle(): Long = requireBound().buffer

    /**
     * Allocates the device buffer (or reallocates it if the source size changed) and uploads the
     * source data for [StorageBuffer.Mode.Input] and [StorageBuffer.Mode.ReadWrite] buffers.
     *
     * Calling [bind] again re-uploads the current source data.
     *
     * @throws KomputeConfigurationException if the buffer is empty or larger than 2 GiB
     */
    override fun bind() {
        val byteSize = elementCount().toLong() * elementSizeInBytes()
        requireConfiguration(byteSize in 1L..Int.MAX_VALUE) {
            "StorageBuffer must be between 1 byte and 2 GiB, was $byteSize bytes"
        }

        val existing = deviceBuffer
        val buffer = if (existing != null && existing.size == byteSize) {
            existing
        } else {
            existing?.close()
            deviceBuffer = null
            context.allocator.createBuffer(byteSize, DEVICE_BUFFER_USAGE, VulkanMemoryKind.DEVICE)
                .also { deviceBuffer = it }
        }

        when (val mode = source.mode()) {
            is StorageBuffer.Mode.Input -> upload(mode.data, buffer)
            is StorageBuffer.Mode.ReadWrite -> upload(mode.data, buffer)
            is StorageBuffer.Mode.Output -> Unit
        }
    }

    private fun upload(
        data: T,
        target: VulkanAllocatedBuffer,
    ) {
        context.allocator.createBuffer(target.size, VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
            VulkanMemoryKind.STAGING_UPLOAD).use { staging ->
            write(data, staging.mapped())
            staging.flush()
            context.commandPool.executeOneTime { commandBuffer ->
                copy(commandBuffer, staging.buffer, target.buffer, target.size)
                commandBuffer.memoryBarrier(
                    VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK10.VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                    VK10.VK_ACCESS_SHADER_READ_BIT or VK10.VK_ACCESS_SHADER_WRITE_BIT,
                )
            }
        }
    }

    /**
     * Copies the device buffer back to host memory.
     *
     * @return a new array of type [T] with the current buffer contents
     * @throws KomputeConfigurationException if the buffer is input-only
     * @throws KomputeBackendInitializationException if [bind] was not called before
     */
    override fun read(): T {
        requireConfiguration(source.isOutput) { "Cannot read an input-only buffer" }
        val device = requireBound()
        context.allocator.createBuffer(device.size, VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT,
            VulkanMemoryKind.STAGING_READBACK).use { staging ->
            context.commandPool.executeOneTime { commandBuffer ->
                commandBuffer.memoryBarrier(
                    VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                    VK10.VK_ACCESS_SHADER_WRITE_BIT,
                    VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK10.VK_ACCESS_TRANSFER_READ_BIT,
                )
                copy(commandBuffer, device.buffer, staging.buffer, device.size)
                commandBuffer.memoryBarrier(
                    VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK10.VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK10.VK_PIPELINE_STAGE_HOST_BIT,
                    VK10.VK_ACCESS_HOST_READ_BIT,
                )
            }
            staging.invalidate()
            return readArray(staging.mapped())
        }
    }

    override fun close() {
        deviceBuffer?.close()
        deviceBuffer = null
    }

    private fun requireBound(): VulkanAllocatedBuffer = deviceBuffer
        ?: throw KomputeBackendInitializationException("bind() must be called before using the buffer")

    private fun copy(
        commandBuffer: VkCommandBuffer,
        from: Long,
        to: Long,
        size: Long,
    ) {
        MemoryStack.stackPush().use { stack ->
            val region = VkBufferCopy.calloc(1, stack)
                .srcOffset(0)
                .dstOffset(0)
                .size(size)
            VK10.vkCmdCopyBuffer(commandBuffer, from, to, region)
        }
    }

    private fun elementCount(): Int = when (val mode = source.mode()) {
        is StorageBuffer.Mode.Input -> countOf(mode.data)
        is StorageBuffer.Mode.ReadWrite -> countOf(mode.data)
        is StorageBuffer.Mode.Output -> mode.size
    }

    private fun countOf(data: T): Int = when (data) {
        is IntArray -> data.size
        is LongArray -> data.size
        is FloatArray -> data.size
        is DoubleArray -> data.size
        is ByteArray -> data.size
        else -> throw unsupportedType()
    }

    private fun elementSizeInBytes(): Int = when (source.type) {
        IntArray::class -> Int.SIZE_BYTES
        LongArray::class -> Long.SIZE_BYTES
        FloatArray::class -> Float.SIZE_BYTES
        DoubleArray::class -> Double.SIZE_BYTES
        ByteArray::class -> 1
        else -> throw unsupportedType()
    }

    private fun write(
        data: T,
        target: ByteBuffer,
    ) {
        when (data) {
            is IntArray -> target.asIntBuffer().put(data)
            is LongArray -> target.asLongBuffer().put(data)
            is FloatArray -> target.asFloatBuffer().put(data)
            is DoubleArray -> target.asDoubleBuffer().put(data)
            is ByteArray -> target.put(data)
            else -> throw unsupportedType()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun readArray(bytes: ByteBuffer): T {
        val count = elementCount()
        return when (source.type) {
            IntArray::class -> IntArray(count).also { bytes.asIntBuffer().get(it) }
            LongArray::class -> LongArray(count).also { bytes.asLongBuffer().get(it) }
            FloatArray::class -> FloatArray(count).also { bytes.asFloatBuffer().get(it) }
            DoubleArray::class -> DoubleArray(count).also { bytes.asDoubleBuffer().get(it) }
            ByteArray::class -> ByteArray(count).also { bytes.get(it) }
            else -> throw unsupportedType()
        } as T
    }

    private fun unsupportedType() = KomputeConfigurationException("Unsupported StorageBuffer type: ${source.type}")

    companion object {
        private const val TRANSFER_USAGE =
            VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT or VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT
        private const val DEVICE_BUFFER_USAGE = VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT or TRANSFER_USAGE
    }
}
