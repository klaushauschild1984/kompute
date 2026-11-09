/**
 * Bundles helpers for recording Vulkan pipeline barriers.
 */

package de.hauschild.kompute.vulkan.backend

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkCommandBuffer
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
