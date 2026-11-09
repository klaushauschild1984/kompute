package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.KomputeBackendDispatchException
import de.hauschild.kompute.core.exception.requireBackendInitialization
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.vma.Vma
import org.lwjgl.util.vma.Vma.vmaCreateAllocator
import org.lwjgl.util.vma.Vma.vmaCreateBuffer
import org.lwjgl.util.vma.Vma.vmaDestroyAllocator
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK13
import org.lwjgl.vulkan.VkBufferCreateInfo

/**
 * Wrapper around the Vulkan Memory Allocator (VMA), used for buffer/image memory management.
 *
 * @param vulkanInstance the [VulkanInstance]
 * @param physicalDevice the [VulkanPhysicalDevice]
 * @param vulkanDevice the [VulkanDevice]
 */
class VulkanAllocator(
    vulkanInstance: VulkanInstance,
    physicalDevice: VulkanPhysicalDevice,
    vulkanDevice: VulkanDevice,
) : AutoCloseable {
    /**
     * The raw VMA allocator handle.
     */
    val allocator: Long

    init {
        MemoryStack.stackPush().use { stack ->
            // LWJGL doesn't statically link VMA against Vulkan; this populates VMA's function
            // pointer table from the capabilities already loaded by lwjgl-vulkan for this
            // instance/device.
            val vulkanFunctions = VmaVulkanFunctions.calloc(stack)
                .set(vulkanInstance.instance, vulkanDevice.device)

            val createInfo = VmaAllocatorCreateInfo.calloc(stack)
                .physicalDevice(physicalDevice.physicalDevice)
                .device(vulkanDevice.device)
                .pVulkanFunctions(vulkanFunctions)
                .instance(vulkanInstance.instance)
                .vulkanApiVersion(VulkanBackend.API_VERSION)

            val allocatorPointer = stack.mallocPointer(1)
            val result = vmaCreateAllocator(createInfo, allocatorPointer)
            requireBackendInitialization(result == VK13.VK_SUCCESS) {
                "Failed to create Vulkan Memory Allocator: ${vkResultName(result)}"
            }
            allocator = allocatorPointer.get(0)
        }
    }

    /**
     * Creates a buffer and binds VMA-managed memory of the requested [kind] to it.
     *
     * @param size the buffer size in bytes, must be greater than zero
     * @param usage the `VkBufferUsageFlags` of the buffer
     * @param kind where the buffer memory should live
     * @return the [VulkanAllocatedBuffer], to be closed by the caller
     * @throws KomputeBackendDispatchException if the allocation fails
     */
    internal fun createBuffer(
        size: Long,
        usage: Int,
        kind: VulkanMemoryKind,
    ): VulkanAllocatedBuffer = MemoryStack.stackPush().use { stack ->
        val bufferInfo = VkBufferCreateInfo.calloc(stack)
            .sType(VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            .size(size)
            .usage(usage)
            .sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE)
        val allocationCreateInfo = VmaAllocationCreateInfo.calloc(stack)
            .usage(Vma.VMA_MEMORY_USAGE_AUTO)
            .flags(kind.allocationFlags)
        val bufferPointer = stack.mallocLong(1)
        val allocationPointer = stack.mallocPointer(1)
        val allocationInfo = VmaAllocationInfo.calloc(stack)

        val result = vmaCreateBuffer(
            allocator, bufferInfo, allocationCreateInfo, bufferPointer, allocationPointer, allocationInfo,
        )
        checkVk(result) { "Failed to allocate Vulkan buffer of $size bytes" }

        VulkanAllocatedBuffer(
            allocator,
            bufferPointer.get(0),
            allocationPointer.get(0),
            size,
            allocationInfo.pMappedData(),
        )
    }

    override fun close() {
        vmaDestroyAllocator(allocator)
    }
}
