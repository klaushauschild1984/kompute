package de.hauschild.kompute.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkWriteDescriptorSet

/**
 * Represents a resource that can be bound for a Vulkan compute dispatch.
 */
interface VulkanBindable : AutoCloseable {
    /**
     * Binds this resource for the upcoming dispatch.
     */
    fun bind()

    /**
     * Describes this resource for a descriptor set update: fills binding index, descriptor type,
     * descriptor count and the buffer or image info. The caller sets `sType` and the target set.
     *
     * @param write the descriptor write to fill
     * @param stack the stack that owns the info structures, it must outlive the descriptor set update
     */
    fun describe(
        write: VkWriteDescriptorSet,
        stack: MemoryStack,
    )
}
