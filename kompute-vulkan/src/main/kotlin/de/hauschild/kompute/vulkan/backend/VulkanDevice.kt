package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.requireBackendInitialization
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.vkCreateDevice
import org.lwjgl.vulkan.VK10.vkDestroyDevice
import org.lwjgl.vulkan.VK10.vkGetDeviceQueue
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkDeviceCreateInfo
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo
import org.lwjgl.vulkan.VkQueue

/**
 * Wrapper around [VkDevice] and [VkQueue].
 *
 * @param physicalDevice the [VulkanPhysicalDevice]
 */
class VulkanDevice(
    physicalDevice: VulkanPhysicalDevice,
) : AutoCloseable {
    /**
     * The raw [VkDevice].
     */
    val device: VkDevice

    /**
     * The raw [VkQueue].
     */
    val queue: VkQueue

    init {
        MemoryStack.stackPush().use { stack ->
            val queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, stack)
                .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(physicalDevice.queueFamilyIndex)
                .pQueuePriorities(stack.floats(1.0f))
            val deviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(queueCreateInfo)
            val devicePointer = stack.mallocPointer(1)
            val result = vkCreateDevice(physicalDevice.physicalDevice, deviceCreateInfo, null, devicePointer)
            requireBackendInitialization(result == VK10.VK_SUCCESS) {
                "Failed to create Vulkan device (VkResult=$result)"
            }
            device = VkDevice(devicePointer.get(0), physicalDevice.physicalDevice, deviceCreateInfo)
            val queuePointer = stack.mallocPointer(1)
            vkGetDeviceQueue(device, physicalDevice.queueFamilyIndex, 0, queuePointer)
            queue = VkQueue(queuePointer.get(0), device)
        }
    }
    override fun close() {
        vkDestroyDevice(device, null)
    }
}
