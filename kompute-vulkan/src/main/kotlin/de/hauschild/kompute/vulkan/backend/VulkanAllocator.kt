package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.requireBackendInitialization
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.vma.Vma.vmaCreateAllocator
import org.lwjgl.util.vma.Vma.vmaDestroyAllocator
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import org.lwjgl.vulkan.VK13

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

    override fun close() {
        vmaDestroyAllocator(allocator)
    }
}
