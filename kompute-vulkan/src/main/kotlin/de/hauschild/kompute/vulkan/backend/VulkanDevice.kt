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
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
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

    /**
     * Whether shaders can use 64 bit floating point types (`double`). Not available on every device,
     * e.g. Metal, and with it MoltenVK on macOS, has no `double`.
     */
    val isFloat64Enabled: Boolean

    /**
     * Whether shaders can use 64 bit integer types (`int64_t`).
     */
    val isInt64Enabled: Boolean

    init {
        MemoryStack.stackPush().use { stack ->
            val queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, stack)
                .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(physicalDevice.queueFamilyIndex)
                .pQueuePriorities(stack.floats(1.0f))
            val supported = VkPhysicalDeviceFeatures.calloc(stack)
            VK10.vkGetPhysicalDeviceFeatures(physicalDevice.physicalDevice, supported)
            isFloat64Enabled = supported.shaderFloat64()
            isInt64Enabled = supported.shaderInt64()
            val deviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(queueCreateInfo)
                .pEnabledFeatures(enabledFeatures(supported, stack))
            val devicePointer = stack.mallocPointer(1)
            val result = vkCreateDevice(physicalDevice.physicalDevice, deviceCreateInfo, null, devicePointer)
            requireBackendInitialization(result == VK10.VK_SUCCESS) {
                "Failed to create Vulkan device: ${vkResultName(result)}"
            }
            device = VkDevice(devicePointer.get(0), physicalDevice.physicalDevice, deviceCreateInfo)
            val queuePointer = stack.mallocPointer(1)
            vkGetDeviceQueue(device, physicalDevice.queueFamilyIndex, 0, queuePointer)
            queue = VkQueue(queuePointer.get(0), device)
        }
    }

    /**
     * Enables the optional features that shaders can need for the data types Kompute supports -
     * 64 bit floats and integers (`DoubleArray`, `LongArray`) and storage images with formats beyond
     * the mandatory ones (e.g. `R8`) - as far as the physical device supports them.
     */
    private fun enabledFeatures(
        supported: VkPhysicalDeviceFeatures,
        stack: MemoryStack,
    ): VkPhysicalDeviceFeatures =
        VkPhysicalDeviceFeatures.calloc(stack)
            .shaderFloat64(supported.shaderFloat64())
            .shaderInt64(supported.shaderInt64())
            .shaderStorageImageExtendedFormats(supported.shaderStorageImageExtendedFormats())

    override fun close() {
        vkDestroyDevice(device, null)
    }
}
