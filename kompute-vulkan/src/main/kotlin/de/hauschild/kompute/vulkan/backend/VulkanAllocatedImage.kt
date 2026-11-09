package de.hauschild.kompute.vulkan.backend

import org.lwjgl.util.vma.Vma

/**
 * A `VkImage` together with its VMA allocation.
 *
 * Instances are created by [VulkanAllocator.createImage]. [close] is idempotent.
 *
 * @property allocator the raw VMA allocator handle this image was created from
 * @property image the raw `VkImage` handle (non-dispatchable, hence a plain [Long])
 * @property allocation the raw VMA allocation handle
 */
internal class VulkanAllocatedImage(
    val allocator: Long,
    val image: Long,
    val allocation: Long,
) : AutoCloseable {
    private var closed = false

    override fun close() {
        if (!closed) {
            closed = true
            Vma.vmaDestroyImage(allocator, image, allocation)
        }
    }
}
