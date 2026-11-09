/**
 * Bundles helpers for recording Vulkan pipeline barriers.
 */

package de.hauschild.kompute.vulkan.backend

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkImageMemoryBarrier
import org.lwjgl.vulkan.VkMemoryBarrier

/**
 * Records a global memory barrier into this command buffer.
 *
 * @receiver the command buffer in recording state
 * @param srcStage the pipeline stages that must complete first
 * @param srcAccess the memory accesses made available by the first scope
 * @param dstStage the pipeline stages that must wait
 * @param dstAccess the memory accesses made visible to the second scope
 */
internal fun VkCommandBuffer.memoryBarrier(
    srcStage: Int,
    srcAccess: Int,
    dstStage: Int,
    dstAccess: Int,
) {
    MemoryStack.stackPush().use { stack ->
        val barrier = VkMemoryBarrier.calloc(1, stack)
            .sType(VK10.VK_STRUCTURE_TYPE_MEMORY_BARRIER)
            .srcAccessMask(srcAccess)
            .dstAccessMask(dstAccess)
        VK10.vkCmdPipelineBarrier(this, srcStage, dstStage, 0, barrier, null, null)
    }
}

/**
 * Records a layout transition (with the memory dependency of a barrier) for the color aspect of a
 * single-mip, single-layer image into this command buffer.
 *
 * @receiver the command buffer in recording state
 * @param image the raw `VkImage` handle
 * @param oldLayout the layout the image is in
 * @param newLayout the layout the image is moved to
 * @param srcStage the pipeline stages that must complete first
 * @param srcAccess the memory accesses made available by the first scope
 * @param dstStage the pipeline stages that must wait
 * @param dstAccess the memory accesses made visible to the second scope
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS") // mirrors the fields of VkImageMemoryBarrier
internal fun VkCommandBuffer.imageBarrier(
    image: Long,
    oldLayout: Int,
    newLayout: Int,
    srcStage: Int,
    srcAccess: Int,
    dstStage: Int,
    dstAccess: Int,
) {
    MemoryStack.stackPush().use { stack ->
        val barrier = VkImageMemoryBarrier.calloc(1, stack)
            .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            .srcAccessMask(srcAccess)
            .dstAccessMask(dstAccess)
            .oldLayout(oldLayout)
            .newLayout(newLayout)
            .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
            .image(image)
        barrier.subresourceRange()
            .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
            .baseMipLevel(0)
            .levelCount(1)
            .baseArrayLayer(0)
            .layerCount(1)
        VK10.vkCmdPipelineBarrier(this, srcStage, dstStage, 0, null, null, barrier)
    }
}
