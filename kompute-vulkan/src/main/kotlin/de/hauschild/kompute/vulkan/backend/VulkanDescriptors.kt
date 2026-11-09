/**
 * Bundles helpers for filling descriptor writes.
 */

package de.hauschild.kompute.vulkan.backend

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDescriptorBufferInfo
import org.lwjgl.vulkan.VkWriteDescriptorSet

/**
 * Describes the whole [buffer] as the resource of a buffer descriptor.
 *
 * @receiver the descriptor write to fill
 * @param binding the binding index in the shader
 * @param type the `VkDescriptorType`, a storage or uniform buffer
 * @param buffer the buffer the descriptor points to
 * @param stack the stack that owns the info structure, it must outlive the descriptor set update
 */
internal fun VkWriteDescriptorSet.describeBuffer(
    binding: Int,
    type: Int,
    buffer: VulkanAllocatedBuffer,
    stack: MemoryStack,
) {
    val bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
        .buffer(buffer.buffer)
        .offset(0)
        .range(VK10.VK_WHOLE_SIZE)
    dstBinding(binding)
        .descriptorType(type)
        .descriptorCount(1)
        .pBufferInfo(bufferInfo)
}
