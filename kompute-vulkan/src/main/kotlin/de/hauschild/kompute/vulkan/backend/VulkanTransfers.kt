/**
 * Bundles the buffer and image copy commands of [VulkanTransferBatch] and the helpers that submit a
 * batch on its own.
 */

package de.hauschild.kompute.vulkan.backend

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkBufferImageCopy
import org.lwjgl.vulkan.VkCommandBuffer

/**
 * Binds a resource in a batch of its own and submits it, for resources that are not part of a dispatch.
 *
 * @receiver the [VulkanContext] providing allocator and queue
 * @param bind schedules the uploads of the resource
 */
internal fun VulkanContext.submitBind(bind: (VulkanTransferBatch) -> Unit) {
    VulkanTransferBatch(this).use { batch ->
        bind(batch)
        batch.submit()
    }
}

/**
 * Reads a resource in a batch of its own: schedules the readback, submits and converts the result.
 *
 * @receiver the [VulkanContext] providing allocator and queue
 * @param schedule schedules the readback and returns the function that converts the downloaded bytes
 * @return the converted result
 */
internal fun <T> VulkanContext.submitRead(schedule: (VulkanTransferBatch) -> () -> T): T =
    VulkanTransferBatch(this).use { batch ->
        val pending = schedule(batch)
        batch.submit()
        pending()
    }

/**
 * Records copying [size] bytes from one buffer to another.
 *
 * @receiver the command buffer in recording state
 * @param from the raw source `VkBuffer` handle
 * @param to the raw destination `VkBuffer` handle
 * @param size the number of bytes to copy
 */
internal fun VkCommandBuffer.copyBuffer(
    from: Long,
    to: Long,
    size: Long,
) {
    MemoryStack.stackPush().use { stack ->
        val region = VkBufferCopy.calloc(1, stack)
            .srcOffset(0)
            .dstOffset(0)
            .size(size)
        VK10.vkCmdCopyBuffer(this, from, to, region)
    }
}

/**
 * Records copying a tightly packed 2D image in `VK_IMAGE_LAYOUT_GENERAL` to a buffer.
 *
 * @receiver the command buffer in recording state
 * @param image the raw `VkImage` handle
 * @param buffer the raw destination `VkBuffer` handle
 * @param width the image width in texels
 * @param height the image height in texels
 */
internal fun VkCommandBuffer.copyImageToBuffer(
    image: Long,
    buffer: Long,
    width: Int,
    height: Int,
) {
    MemoryStack.stackPush().use { stack ->
        val region = VkBufferImageCopy.calloc(1, stack)
            .bufferOffset(0)
            .bufferRowLength(0)
            .bufferImageHeight(0)
        region.imageSubresource()
            .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
            .mipLevel(0)
            .baseArrayLayer(0)
            .layerCount(1)
        region.imageExtent()
            .width(width)
            .height(height)
            .depth(1)
        VK10.vkCmdCopyImageToBuffer(this, image, VK10.VK_IMAGE_LAYOUT_GENERAL, buffer, region)
    }
}
