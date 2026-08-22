package de.hauschild.kompute.vulkan

/**
 * Represents a resource that can be bound for a Vulkan compute dispatch.
 */
interface VulkanBindable : AutoCloseable {
    /**
     * Binds this resource for the upcoming dispatch.
     */
    fun bind()
}
