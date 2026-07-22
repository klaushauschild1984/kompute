package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.InternalApi
import de.hauschild.kompute.core.backend.AbstractBackend
import de.hauschild.kompute.core.backend.Type
import de.hauschild.kompute.core.shader.CompiledShader
import de.hauschild.kompute.core.shader.ShaderSource
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.vulkan.VK13

/**
 * Vulkan compute backend implementation using LWJGL.
 *
 * Creates a Vulkan instance, selects a compute-capable physical device, and sets up a logical
 * device with a dedicated compute queue and command pool. Buffer and image memory is managed via
 * the Vulkan Memory Allocator (VMA). An opt-in validation layer enables detailed API usage
 * checking during development.
 */
class VulkanBackend : AbstractBackend() {
    private lateinit var vulkanInstance: VulkanInstance
    private lateinit var vulkanPhysicalDevice: VulkanPhysicalDevice
    private lateinit var vulkanDevice: VulkanDevice
    private lateinit var vulkanCommandPool: VulkanCommandPool
    private lateinit var vulkanAllocator: VulkanAllocator

    @InternalApi
    override fun type(): Type = Type.Vulkan
    override fun doInitialize() {
        vulkanInstance = VulkanInstance()
        vulkanPhysicalDevice = VulkanPhysicalDevice(vulkanInstance)
        vulkanDevice = VulkanDevice(vulkanPhysicalDevice)
        vulkanCommandPool = VulkanCommandPool(vulkanDevice, vulkanPhysicalDevice)
        vulkanAllocator = VulkanAllocator(vulkanInstance, vulkanPhysicalDevice, vulkanDevice)

        logger.info {
            "Vulkan Backend initialized with device: ${vulkanPhysicalDevice.name()}, " +
                    "apiVersion: ${vulkanPhysicalDevice.apiVersion()}, " +
                    "deviceLocalMemory: ${vulkanPhysicalDevice.deviceLocalMemory() / BYTES_PER_GB} GB, " +
                    "driverVersion: ${vulkanPhysicalDevice.driverVersion()}"
        }
    }

    override fun compileSource(source: ShaderSource): CompiledShader {
        TODO("Not yet implemented")
    }

    override fun close() {
        if (::vulkanAllocator.isInitialized) {
            vulkanAllocator.close()
        }
        if (::vulkanCommandPool.isInitialized) {
            vulkanCommandPool.close()
        }
        if (::vulkanDevice.isInitialized) {
            vulkanDevice.close()
        }
        if (::vulkanInstance.isInitialized) {
            vulkanInstance.close()
        }

        logger.debug { "Vulkan Backend closed" }
    }

    companion object {
        private const val BYTES_PER_GB = 1_024 * 1_024 * 1_024
        private val logger = KotlinLogging.logger {}

        /**
         * The Vulkan API version this backend targets.
         */
        val API_VERSION = VK13.VK_API_VERSION_1_3
    }
}
