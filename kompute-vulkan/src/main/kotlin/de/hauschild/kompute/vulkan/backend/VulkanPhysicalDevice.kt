package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.requireBackendInitialization
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_API_VERSION_MAJOR
import org.lwjgl.vulkan.VK10.VK_API_VERSION_MINOR
import org.lwjgl.vulkan.VK10.VK_API_VERSION_PATCH
import org.lwjgl.vulkan.VK10.VK_MEMORY_HEAP_DEVICE_LOCAL_BIT
import org.lwjgl.vulkan.VK10.VK_QUEUE_COMPUTE_BIT
import org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VkPhysicalDeviceProperties
import org.lwjgl.vulkan.VkQueueFamilyProperties
import kotlin.use

/**
 * Wrapper around [VkPhysicalDevice] and the queue family index.
 *
 * @param instance the [VulkanInstance]
 */
class VulkanPhysicalDevice(
    instance: VulkanInstance
) {
    /**
     * The raw [VkPhysicalDevice].
     */
    val physicalDevice: VkPhysicalDevice

    /**
     * The queue family index (relative to the physical device)
     */
    val queueFamilyIndex: Int
    private val properties: Properties by lazy {
        MemoryStack.stackPush().use { stack ->
            val properties = VkPhysicalDeviceProperties.malloc(stack)
            vkGetPhysicalDeviceProperties(physicalDevice, properties)
            val deviceNameString = properties.deviceNameString()
            val apiVersion = properties.apiVersion()
            val major = VK_API_VERSION_MAJOR(apiVersion)
            val minor = VK_API_VERSION_MINOR(apiVersion)
            val patch = VK_API_VERSION_PATCH(apiVersion)
            val driverVersion = properties.driverVersion()

            val memoryProperties = VkPhysicalDeviceMemoryProperties.malloc(stack)
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties)
            val deviceLocalMemory = (0 until memoryProperties.memoryHeapCount())
                .map { memoryProperties.memoryHeaps(it) }
                .filter { it.flags() and VK_MEMORY_HEAP_DEVICE_LOCAL_BIT != 0 }
                .sumOf { it.size() }

            Properties(deviceNameString, "$major.$minor.$patch", deviceLocalMemory, driverVersion)
        }
    }

    init {
        val (physicalDevice, queueFamilyIndex) = pickComputeCapableDevice(instance)
        this.physicalDevice = physicalDevice
        this.queueFamilyIndex = queueFamilyIndex
    }

    /**
     * The device name.
     */
    fun name(): String = properties.name

    /**
     * The API version.
     */
    fun apiVersion(): String = properties.apiVersion

    /**
     * The total device-local memory in bytes.
     */
    fun deviceLocalMemory(): Long = properties.deviceLocalMemory

    /**
     * The raw, vendor-specific driver version.
     */
    fun driverVersion(): Int = properties.driverVersion

    private fun pickComputeCapableDevice(instance: VulkanInstance): Pair<VkPhysicalDevice, Int> {
        MemoryStack.stackPush().use { stack ->
            val deviceCount = stack.mallocInt(1)
            vkEnumeratePhysicalDevices(instance.instance, deviceCount, null)
            requireBackendInitialization(deviceCount.get(0) > 0) {
                "No Vulkan-capable physical devices found"
            }

            val devicePointers = stack.mallocPointer(deviceCount.get(0))
            vkEnumeratePhysicalDevices(instance.instance, deviceCount, devicePointers)

            val candidate = (0 until devicePointers.capacity())
                .map { VkPhysicalDevice(devicePointers.get(it), instance.instance) }
                .firstNotNullOfOrNull { physicalDevice ->
                    findComputeQueueFamily(physicalDevice, stack)?.let { physicalDevice to it }
                }

            requireBackendInitialization(candidate != null) { "No compute-capable Vulkan device found" }
            return candidate!!
        }
    }

    private fun findComputeQueueFamily(physicalDevice: VkPhysicalDevice, stack: MemoryStack): Int? {
        val queueFamilyCount = stack.mallocInt(1)
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null)

        val queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack)
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, queueFamilies)

        // TODO sort devices by
        // 1. device type rank
        // 2. available memory
        // 3. supported API version
        // force device via system property
        // force compute-only via system property
        return (0 until queueFamilies.capacity())
            .firstOrNull { index -> queueFamilies[index].queueFlags() and VK_QUEUE_COMPUTE_BIT != 0 }
    }

    /**
     * Physical device properties.
     *
     * @property name physical device name
     * @property apiVersion API version
     * @property deviceLocalMemory total device-local memory in bytes
     * @property driverVersion raw, vendor-specific driver version
     */
    private data class Properties(
        val name: String,
        val apiVersion: String,
        val deviceLocalMemory: Long,
        val driverVersion: Int,
    )
}
