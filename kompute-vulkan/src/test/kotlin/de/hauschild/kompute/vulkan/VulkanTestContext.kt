package de.hauschild.kompute.vulkan

import de.hauschild.kompute.vulkan.backend.VulkanAllocator
import de.hauschild.kompute.vulkan.backend.VulkanCommandPool
import de.hauschild.kompute.vulkan.backend.VulkanContext
import de.hauschild.kompute.vulkan.backend.VulkanDevice
import de.hauschild.kompute.vulkan.backend.VulkanInstance
import de.hauschild.kompute.vulkan.backend.VulkanLimits
import de.hauschild.kompute.vulkan.backend.VulkanPhysicalDevice

/**
 * Creates the Vulkan objects behind a [VulkanContext] without a backend, so that resources can be
 * tested on their own. Honors the system property `kompute.backend.vulkan.validation` like the backend.
 */
class VulkanTestContext : AutoCloseable {
    private val instance = VulkanInstance(System.getProperty("kompute.backend.vulkan.validation").toBoolean())
    private val physicalDevice = VulkanPhysicalDevice(false, null, instance)
    private val vulkanDevice = VulkanDevice(physicalDevice)
    private val commandPool = VulkanCommandPool(vulkanDevice, physicalDevice)
    private val allocator = VulkanAllocator(instance, physicalDevice, vulkanDevice)
    private val directAccessByDefault = System.getProperty("kompute.backend.vulkan.directAccess")?.toBoolean() ?: true

    /**
     * The device the contexts were created for.
     */
    val device: VulkanDevice = vulkanDevice

    /**
     * The context under test.
     */
    val context = VulkanContext(vulkanDevice, allocator, commandPool, physicalDevice.limits(), directAccessByDefault)

    /**
     * Creates a second context on the same device with other limits, e.g. artificially small ones, so
     * that limit validation can be tested independent of the hardware. It has to be closed.
     *
     * @param limits the limits the new context reports
     * @param directAccess whether its persistent buffers may use directly accessible memory
     * @return the new context
     */
    fun contextWith(
        limits: VulkanLimits,
        directAccess: Boolean = directAccessByDefault,
    ) = VulkanContext(vulkanDevice, allocator, commandPool, limits, directAccess)

    /**
     * Creates a second command pool with a fence timeout. It has to be closed.
     *
     * @param fenceTimeoutNanos how long to wait for a submission in nanoseconds
     * @return the new command pool
     */
    fun commandPoolWithTimeout(fenceTimeoutNanos: Long) =
        VulkanCommandPool(vulkanDevice, physicalDevice, fenceTimeoutNanos)

    override fun close() {
        context.close()
        allocator.close()
        commandPool.close()
        vulkanDevice.close()
        instance.close()
    }
}
