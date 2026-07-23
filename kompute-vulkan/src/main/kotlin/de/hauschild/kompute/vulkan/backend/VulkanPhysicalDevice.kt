package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.requireBackendInitialization
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_API_VERSION_MAJOR
import org.lwjgl.vulkan.VK10.VK_API_VERSION_MINOR
import org.lwjgl.vulkan.VK10.VK_API_VERSION_PATCH
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
 * @param dedicatedComputeQueue controls if a dedicated compute queue without graphics capabilities ('true') is
 * requested.
 * @param deviceName optional case-insensitive substring filter for the physical device name; if set, only
 * matching devices are considered, and initialization fails if none match.
 * @param instance the [VulkanInstance].
 */
class VulkanPhysicalDevice(
    dedicatedComputeQueue: Boolean,
    deviceName: String?,
    instance: VulkanInstance,
) {
    /**
     * The raw [VkPhysicalDevice].
     */
    val physicalDevice: VkPhysicalDevice

    /**
     * The queue family index (relative to the physical device).
     */
    val queueFamilyIndex: Int
    private val properties: Properties by lazy {
        MemoryStack.stackPush().use { stack ->
            val deviceProperties = VkPhysicalDeviceProperties.malloc(stack)
            vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties)
            val deviceNameString = deviceProperties.deviceNameString()
            val apiVersion = deviceProperties.apiVersion()
            val major = VK_API_VERSION_MAJOR(apiVersion)
            val minor = VK_API_VERSION_MINOR(apiVersion)
            val patch = VK_API_VERSION_PATCH(apiVersion)
            val driverVersion = deviceProperties.driverVersion()

            Properties(
                deviceNameString,
                "$major.$minor.$patch",
                queryDeviceLocalMemory(physicalDevice, stack),
                driverVersion,
            )
        }
    }

    init {
        val (physicalDevice, queueFamilyIndex) = pickComputeCapableDevice(dedicatedComputeQueue, deviceName, instance)
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

    private fun pickComputeCapableDevice(
        dedicatedComputeQueue: Boolean,
        deviceName: String?,
        instance: VulkanInstance,
    ): PhysicalDeviceProperties {
        if (dedicatedComputeQueue) {
            logger.debug { "Dedicated compute queue requested." }
        }

        MemoryStack.stackPush().use { stack ->
            val deviceCount = stack.mallocInt(1)
            vkEnumeratePhysicalDevices(instance.instance, deviceCount, null)
            requireBackendInitialization(deviceCount.get(0) > 0) {
                "No Vulkan-capable physical devices found"
            }

            val devicePointers = stack.mallocPointer(deviceCount.get(0))
            vkEnumeratePhysicalDevices(instance.instance, deviceCount, devicePointers)

            val physicalDevices = (0 until devicePointers.capacity())
                .map { VkPhysicalDevice(devicePointers.get(it), instance.instance) }
                .let { filterByDeviceName(it, deviceName, stack) }

            val candidate = iteratePhysicalDevices(physicalDevices, dedicatedComputeQueue, stack)
                ?: run {
                    logger.warn { "No dedicated compute queue family found, falling back to a combined queue family." }
                    iteratePhysicalDevices(physicalDevices, false, stack)
                }

            requireBackendInitialization(candidate != null) { "No compute-capable Vulkan device found" }
            return candidate!!
        }
    }

    private fun filterByDeviceName(
        physicalDevices: List<VkPhysicalDevice>,
        deviceName: String?,
        stack: MemoryStack,
    ): List<VkPhysicalDevice> = deviceName?.let { name ->
        logger.debug { "Filtering Vulkan devices by name: $name" }
        val filtered = physicalDevices.filter { physicalDevice ->
            val deviceProperties = VkPhysicalDeviceProperties.malloc(stack)
            vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties)
            isDeviceNameMatch(deviceProperties.deviceNameString(), name)
        }
        requireBackendInitialization(filtered.isNotEmpty()) {
            "No Vulkan device found matching name filter '$name'"
        }
        filtered
    } ?: physicalDevices

    private fun iteratePhysicalDevices(
        physicalDevices: List<VkPhysicalDevice>,
        dedicatedComputeQueue: Boolean,
        stack: MemoryStack,
    ): PhysicalDeviceProperties? = physicalDevices
        .mapNotNull { physicalDevice ->
            findComputeQueueFamily(dedicatedComputeQueue, physicalDevice, stack)?.let { queueFamilyIndex ->
                val deviceProperties = VkPhysicalDeviceProperties.malloc(stack)
                vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties)

                PhysicalDeviceProperties(
                    physicalDevice,
                    queueFamilyIndex,
                    deviceProperties.deviceType(),
                    queryDeviceLocalMemory(physicalDevice, stack),
                    deviceProperties.apiVersion(),
                )
            }
        }
        .maxWithOrNull(
            compareBy(
                { deviceTypeRank(it.type) },
                { it.deviceLocalMemory },
                { it.apiVersion },
            ),
        )

    private fun queryDeviceLocalMemory(physicalDevice: VkPhysicalDevice, stack: MemoryStack): Long {
        val memoryProperties = VkPhysicalDeviceMemoryProperties.malloc(stack)
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties)
        val heaps = (0 until memoryProperties.memoryHeapCount())
            .map { memoryProperties.memoryHeaps(it) }
            .map { HeapInfo(it.flags(), it.size()) }
        return sumDeviceLocalMemory(heaps)
    }

    private fun findComputeQueueFamily(
        dedicatedComputeQueue: Boolean,
        physicalDevice: VkPhysicalDevice,
        stack: MemoryStack,
    ): Int? {
        val queueFamilyCount = stack.mallocInt(1)
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null)

        val queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack)
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, queueFamilies)

        // TODO force device via system property
        return (0 until queueFamilies.capacity())
            .firstOrNull { index ->
                isComputeRequirementSatisfied(queueFamilies[index].queueFlags(), dedicatedComputeQueue)
            }
    }

    /**
     * A compute-capable physical device candidate, gathered during device selection.
     *
     * @property physicalDevice the raw [VkPhysicalDevice].
     * @property queueFamilyIndex the compute queue family index (relative to the physical device).
     * @property type the raw `VkPhysicalDeviceType` value.
     * @property deviceLocalMemory total device-local memory in bytes.
     * @property apiVersion the raw, packed Vulkan API version.
     */
    private data class PhysicalDeviceProperties(
        val physicalDevice: VkPhysicalDevice,
        val queueFamilyIndex: Int,
        val type: Int,
        val deviceLocalMemory: Long,
        val apiVersion: Int,
    )

    /**
     * Physical device properties.
     *
     * @property name physical device name.
     * @property apiVersion API version.
     * @property deviceLocalMemory total device-local memory in bytes.
     * @property driverVersion raw, vendor-specific driver version.
     */
    private data class Properties(
        val name: String,
        val apiVersion: String,
        val deviceLocalMemory: Long,
        val driverVersion: Int,
    )

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
