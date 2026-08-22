package de.hauschild.kompute.vulkan.data

import de.hauschild.kompute.core.data.UniformBufferObject
import de.hauschild.kompute.vulkan.VulkanBuffer

/**
 * Wraps a Vulkan uniform buffer (UBO) for a [UniformBufferObject].
 *
 * @param source the [UniformBufferObject] configuration this buffer is based on
 */
class VulkanUniformBufferObject(
    source: UniformBufferObject,
) : VulkanBuffer<UniformBufferObject>(source) {
    override fun bind() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}
