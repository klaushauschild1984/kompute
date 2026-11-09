package de.hauschild.kompute.vulkan.data

import de.hauschild.kompute.core.data.UniformBufferObject
import de.hauschild.kompute.vulkan.VulkanBuffer
import de.hauschild.kompute.vulkan.backend.VulkanContext
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkWriteDescriptorSet

/**
 * Wraps a Vulkan uniform buffer (UBO) for a [UniformBufferObject].
 *
 * @param source the [UniformBufferObject] configuration this buffer is based on
 * @param context the [VulkanContext] used for memory allocation and transfer commands
 */
@Suppress("UnusedPrivateProperty") // not implemented yet
class VulkanUniformBufferObject(
    source: UniformBufferObject,
    context: VulkanContext,
) : VulkanBuffer<UniformBufferObject>(source) {
    override fun bind() {
        TODO("Not yet implemented")
    }

    override fun describe(
        write: VkWriteDescriptorSet,
        stack: MemoryStack,
    ) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}
