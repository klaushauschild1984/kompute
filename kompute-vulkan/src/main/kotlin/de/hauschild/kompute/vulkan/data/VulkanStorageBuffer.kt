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
import de.hauschild.kompute.vulkan.backend.VulkanTransferBatch
import de.hauschild.kompute.vulkan.backend.describeBuffer
import de.hauschild.kompute.vulkan.backend.submitRead
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkWriteDescriptorSet

/**
 * Wraps a Vulkan shader storage buffer (SSBO) for a [StorageBuffer].
 *
 * The buffer lives in device-local memory and is persistent, like the OpenGL backend's buffer: it
 * is allocated on the first [bind] for a given [StorageBuffer] instance and reused by all later
 * dispatches, so GPU-side state survives between them. It is released when the source becomes
 * unreachable or the backend is closed, [close] only drops this instance's reference.
 *
 * Data is moved between host and device through temporary staging buffers: the first [bind]
 * uploads the source data, [read] copies the device buffer into a host-visible staging buffer and
 * converts it back into [T]. Both operations block until the GPU has finished the transfer. As in
 * the OpenGL backend, changing the data of the source afterwards does not update the buffer unless
 * the size changes.
 *
 * Note: [LongArray] and [DoubleArray] require the matching shader features (`int64`, `float64`).
 *
 * @param T the data type — must be [IntArray], [LongArray], [FloatArray], [DoubleArray], or [ByteArray]
 * @param source the [StorageBuffer] configuration this buffer is based on
 * @param context the [VulkanContext] used for memory allocation and transfer commands
 */
class VulkanStorageBuffer<T : Any>(
    source: StorageBuffer<T>,
    context: VulkanContext,
) : VulkanBuffer<StorageBuffer<T>>(source, context),
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
     * Looks up the persistent device buffer of the source, allocating it if it does not exist yet (or
     * if the source size changed), and schedules uploading the source data into a newly allocated
     * buffer for [StorageBuffer.Mode.Input] and [StorageBuffer.Mode.ReadWrite] buffers.
     *
     * @throws KomputeConfigurationException if the buffer is empty or exceeds the device limits
     */
    override fun scheduleBind(batch: VulkanTransferBatch) {
        val byteSize = elementCount().toLong() * StorageBufferCodec.elementSizeInBytes(source.type)
        requireConfiguration(byteSize in 1L..Int.MAX_VALUE) {
            "StorageBuffer must be between 1 byte and 2 GiB, was $byteSize bytes"
        }
        requireConfiguration(byteSize <= context.limits.maxStorageBufferRange) {
            "StorageBuffer of $byteSize bytes exceeds the device limit of ${context.limits.maxStorageBufferRange} bytes"
        }

        val (buffer, isNew) = context.bufferCache.getOrAllocate(source, byteSize, DEVICE_BUFFER_USAGE)
        deviceBuffer = buffer
        batch.use(buffer)
        if (isNew) {
            when (val mode = source.mode()) {
                is StorageBuffer.Mode.Input -> upload(batch, mode.data, buffer)
                is StorageBuffer.Mode.ReadWrite -> upload(batch, mode.data, buffer)
                is StorageBuffer.Mode.Output -> Unit
            }
        }
    }

    private fun upload(
        batch: VulkanTransferBatch,
        data: T,
        target: VulkanAllocatedBuffer,
    ) = batch.upload(target) { staging -> StorageBufferCodec.write(data, staging) }

    /**
     * Copies the device buffer back to host memory in a submission of its own.
     *
     * @return a new array of type [T] with the current buffer contents
     * @throws KomputeConfigurationException if the buffer is input-only
     * @throws KomputeBackendInitializationException if [bind] was not called before
     */
    override fun read(): T = context.submitRead { batch -> scheduleRead(batch) }

    override fun scheduleRead(batch: VulkanTransferBatch): () -> T {
        requireConfiguration(source.isOutput) { "Cannot read an input-only buffer" }
        val count = elementCount()
        val download = batch.download(requireBound())
        @Suppress("UNCHECKED_CAST")
        return { StorageBufferCodec.read(source.type, count, download.bytes()) as T }
    }

    override fun descriptorKey(): Any = source.index to requireBound()

    override fun describe(
        write: VkWriteDescriptorSet,
        stack: MemoryStack,
    ) = write.describeBuffer(source.index, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, requireBound(), stack)

    override fun close() {
        // the buffer is persistent: its lifetime is managed by the buffer cache
        deviceBuffer = null
    }

    private fun requireBound(): VulkanAllocatedBuffer = deviceBuffer
        ?: throw KomputeBackendInitializationException("bind() must be called before using the buffer")

    private fun elementCount(): Int = when (val mode = source.mode()) {
        is StorageBuffer.Mode.Input -> StorageBufferCodec.elementCount(mode.data)
        is StorageBuffer.Mode.ReadWrite -> StorageBufferCodec.elementCount(mode.data)
        is StorageBuffer.Mode.Output -> mode.size
    }

    companion object {
        private const val TRANSFER_USAGE =
            VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT or VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT
        private const val DEVICE_BUFFER_USAGE = VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT or TRANSFER_USAGE
    }
}
