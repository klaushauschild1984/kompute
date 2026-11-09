/**
 * Bundles blocking host <-> device buffer transfers through temporary staging buffers.
 */

package de.hauschild.kompute.vulkan.backend

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkCommandBuffer
import java.nio.ByteBuffer

/**
 * Fills [target] with the bytes written by [write]. Blocks until the GPU has finished the copy and
 * makes the data visible to compute shaders.
 *
 * @receiver the [VulkanContext] providing allocator and queue
 * @param target the device-local buffer, must have been created with `TRANSFER_DST` usage
 * @param write writes up to [VulkanAllocatedBuffer.size] bytes into the given staging memory
 */
internal fun VulkanContext.upload(
    target: VulkanAllocatedBuffer,
    write: (ByteBuffer) -> Unit,
) {
    allocator.createBuffer(
        target.size,
        VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
        VulkanMemoryKind.STAGING_UPLOAD,
    ).use { staging ->
        write(staging.mapped())
        staging.flush()
        commandPool.executeOneTime { commandBuffer ->
            commandBuffer.copyBuffer(staging.buffer, target.buffer, target.size)
            commandBuffer.memoryBarrier(
                VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK10.VK_ACCESS_TRANSFER_WRITE_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_ACCESS_SHADER_READ_BIT or VK10.VK_ACCESS_SHADER_WRITE_BIT,
            )
        }
    }
}

/**
 * Copies [source] to host memory and hands the bytes to [read]. Blocks until the GPU has finished
 * all shader writes to [source] and the copy.
 *
 * @receiver the [VulkanContext] providing allocator and queue
 * @param source the device-local buffer, must have been created with `TRANSFER_SRC` usage
 * @param read converts the bytes, only valid for the duration of the call
 * @return the result of [read]
 */
internal fun <R> VulkanContext.download(
    source: VulkanAllocatedBuffer,
    read: (ByteBuffer) -> R,
): R = allocator.createBuffer(
    source.size,
    VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT,
    VulkanMemoryKind.STAGING_READBACK,
).use { staging ->
    commandPool.executeOneTime { commandBuffer ->
        commandBuffer.memoryBarrier(
            VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
            VK10.VK_ACCESS_SHADER_WRITE_BIT,
            VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
            VK10.VK_ACCESS_TRANSFER_READ_BIT,
        )
        commandBuffer.copyBuffer(source.buffer, staging.buffer, source.size)
        commandBuffer.memoryBarrier(
            VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
            VK10.VK_ACCESS_TRANSFER_WRITE_BIT,
            VK10.VK_PIPELINE_STAGE_HOST_BIT,
            VK10.VK_ACCESS_HOST_READ_BIT,
        )
    }
    staging.invalidate()
    read(staging.mapped())
}

private fun VkCommandBuffer.copyBuffer(
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
