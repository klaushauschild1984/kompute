package de.hauschild.kompute.vulkan.data

import de.hauschild.kompute.core.data.UniformBufferObject
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import de.hauschild.kompute.core.exception.requireConfiguration
import de.hauschild.kompute.vulkan.VulkanBuffer
import de.hauschild.kompute.vulkan.backend.VulkanAllocatedBuffer
import de.hauschild.kompute.vulkan.backend.VulkanContext
import de.hauschild.kompute.vulkan.backend.VulkanMemoryKind
import de.hauschild.kompute.vulkan.backend.VulkanTransferBatch
import de.hauschild.kompute.vulkan.backend.describeBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkWriteDescriptorSet

/**
 * Wraps a Vulkan uniform buffer (UBO) for a [UniformBufferObject].
 *
 * The buffer lives in device-local memory and is filled through a temporary staging buffer in [bind].
 * The bytes are uploaded as they are, the caller is responsible for the std140 layout.
 *
 * @param source the [UniformBufferObject] configuration this buffer is based on
 * @param context the [VulkanContext] used for memory allocation and transfer commands
 */
class VulkanUniformBufferObject(
    source: UniformBufferObject,
    context: VulkanContext,
) : VulkanBuffer<UniformBufferObject>(source, context) {
    private var deviceBuffer: VulkanAllocatedBuffer? = null

    /**
     * Allocates the device buffer (or reallocates it if the data size changed) and schedules uploading
     * the data.
     *
     * Calling [bind] again re-uploads the current source data.
     *
     * @throws KomputeConfigurationException if no data is set, the data is empty or exceeds the device limit
     */
    override fun scheduleBind(batch: VulkanTransferBatch) {
        val data = checkNotNull(source.data) { "UniformBufferObject data must be set" }
        requireConfiguration(data.isNotEmpty()) { "UniformBufferObject data must not be empty" }
        val maxRange = context.limits.maxUniformBufferRange
        requireConfiguration(data.size <= maxRange) {
            "UniformBufferObject of ${data.size} bytes exceeds the device limit of $maxRange bytes"
        }

        val existing = deviceBuffer
        val buffer = if (existing != null && existing.size == data.size.toLong()) {
            existing
        } else {
            existing?.close()
            deviceBuffer = null
            context.allocator.createBuffer(data.size.toLong(), BUFFER_USAGE, VulkanMemoryKind.DEVICE)
                .also { deviceBuffer = it }
        }
        batch.upload(buffer) { staging -> staging.put(data) }
    }

    override fun descriptorKey(): Any? = null

    override fun describe(
        write: VkWriteDescriptorSet,
        stack: MemoryStack,
    ) {
        val buffer = checkNotNull(deviceBuffer) { "bind() must be called before describe()" }
        write.describeBuffer(source.index, VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, buffer, stack)
    }

    override fun close() {
        deviceBuffer?.close()
        deviceBuffer = null
    }

    companion object {
        private const val BUFFER_USAGE =
            VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT or VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT
    }
}
