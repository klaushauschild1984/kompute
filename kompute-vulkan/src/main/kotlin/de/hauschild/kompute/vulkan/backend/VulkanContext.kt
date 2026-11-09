package de.hauschild.kompute.vulkan.backend

/**
 * Bundles the backend-level Vulkan objects that GPU resources (buffers, images) need to allocate
 * memory and to record and submit transfer commands.
 *
 * The context does not own its members; their lifetime is managed by the [VulkanBackend].
 *
 * @property device the [VulkanDevice]
 * @property allocator the [VulkanAllocator] used for buffer and image memory
 * @property commandPool the [VulkanCommandPool] used for one-time transfer commands
 */
class VulkanContext(
    val device: VulkanDevice,
    val allocator: VulkanAllocator,
    val commandPool: VulkanCommandPool,
)
