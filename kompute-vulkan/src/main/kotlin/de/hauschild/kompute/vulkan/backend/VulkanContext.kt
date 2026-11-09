package de.hauschild.kompute.vulkan.backend

/**
 * Bundles the backend-level Vulkan objects that GPU resources (buffers, images) need to allocate
 * memory and to record and submit transfer commands, plus the cache of persistent buffers.
 *
 * The context does not own the device, allocator and command pool, their lifetime is managed by the
 * [VulkanBackend]. It owns the persistent buffers: [close] releases them and must be called before
 * the allocator is closed.
 *
 * @property device the [VulkanDevice]
 * @property allocator the [VulkanAllocator] used for buffer and image memory
 * @property commandPool the [VulkanCommandPool] used for one-time transfer commands
 * @property limits the [VulkanLimits] of the physical device
 */
class VulkanContext(
    val device: VulkanDevice,
    val allocator: VulkanAllocator,
    val commandPool: VulkanCommandPool,
    val limits: VulkanLimits,
) : AutoCloseable {
    /**
     * Device-local buffers that outlive a single dispatch, keyed by the shader data they belong to.
     */
    internal val bufferCache = VulkanBufferCache(allocator)

    override fun close() {
        bufferCache.close()
    }
}
