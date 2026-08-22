package de.hauschild.kompute.vulkan.data

import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.vulkan.VulkanBuffer
import de.hauschild.kompute.vulkan.VulkanReadable

/**
 * Wraps a Vulkan storage image for a [Image2D].
 *
 * @param source the [Image2D] configuration this image is based on
 */
class VulkanImage2D(
    source: Image2D,
) : VulkanBuffer<Image2D>(source),
OutputCapable<Image2D.Image2DResult> by source,
VulkanReadable<Image2D.Image2DResult> {
    override fun bind() {
        TODO("Not yet implemented")
    }

    override fun read(): Image2D.Image2DResult {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}
