package de.hauschild.kompute.vulkan.data

import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.exception.KomputeBackendInitializationException
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
 * Wraps an [AtomicCounter] as a persistent storage buffer holding a single `Int`.
 *
 * Vulkan has no dedicated atomic-counter resource type - the shader-side equivalent is a regular
 * storage buffer with an atomic operation on one of its members (see `VULKAN-TODO.md`). Like the
 * OpenGL backend's `OpenGLAtomicCounter`, the buffer is allocated once per [AtomicCounter] instance
 * and reused, but the counter is reset to [AtomicCounter.start] on every [bind], preserving
 * per-dispatch reset semantics. [close] only drops this instance's reference.
 *
 * @param source the [AtomicCounter] configuration this counter is based on
 * @param context the [VulkanContext] used for memory allocation and transfer commands
 */
class VulkanAtomicCounter(
    source: AtomicCounter,
    context: VulkanContext,
) : VulkanBuffer<AtomicCounter>(source, context),
OutputCapable<Int> by source,
VulkanReadable<Int> {
    private var deviceBuffer: VulkanAllocatedBuffer? = null

    override fun scheduleBind(batch: VulkanTransferBatch) {
        val (buffer, _) = context.bufferCache.getOrAllocate(source, Int.SIZE_BYTES.toLong(), BUFFER_USAGE)
        deviceBuffer = buffer
        batch.use(buffer)
        batch.upload(buffer) { staging -> staging.putInt(source.start) }
    }

    /**
     * Reads the current counter value back from the GPU buffer.
     *
     * @return the counter value after shader execution
     * @throws KomputeBackendInitializationException if [bind] was not called before
     */
    override fun read(): Int = context.submitRead { batch -> scheduleRead(batch) }

    override fun scheduleRead(batch: VulkanTransferBatch): () -> Int {
        val download = batch.download(requireBound())
        return { download.bytes().getInt(0) }
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
        ?: throw KomputeBackendInitializationException("bind() must be called before using the counter")

    companion object {
        private const val TRANSFER_USAGE =
            VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT or VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT
        private const val BUFFER_USAGE = VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT or TRANSFER_USAGE
    }
}
