package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.requireBackendInitialization
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.vkCreateCommandPool
import org.lwjgl.vulkan.VK10.vkDestroyCommandPool
import org.lwjgl.vulkan.VkCommandPoolCreateInfo

/**
 * Wrapper around the Vulkan command pool for the compute queue family.
 *
 * @param vulkanDevice the [VulkanDevice]
 * @param physicalDevice the [VulkanPhysicalDevice]
 */
class VulkanCommandPool(
    private val vulkanDevice: VulkanDevice,
    physicalDevice: VulkanPhysicalDevice,
) : AutoCloseable {
    /**
     * The raw command pool handle (non-dispatchable, hence a plain [Long]).
     */
    val commandPool: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val createInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(physicalDevice.queueFamilyIndex)

            val commandPoolPointer = stack.mallocLong(1)
            val result = vkCreateCommandPool(vulkanDevice.device, createInfo, null, commandPoolPointer)
            requireBackendInitialization(result == VK10.VK_SUCCESS) {
                "Failed to create Vulkan command pool: ${vkResultName(result)}"
            }
            commandPool = commandPoolPointer.get(0)
        }
    }

    override fun close() {
        vkDestroyCommandPool(vulkanDevice.device, commandPool, null)
    }
}
