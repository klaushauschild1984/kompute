package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.InternalApi
import de.hauschild.kompute.core.backend.AbstractBackend
import de.hauschild.kompute.core.backend.Type
import de.hauschild.kompute.core.shader.CompiledShader
import de.hauschild.kompute.core.shader.ShaderSource
import de.hauschild.kompute.vulkan.shader.VulkanCompiledShader
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.vulkan.VK13

/**
 * Vulkan compute backend implementation using LWJGL.
 *
 * Creates a Vulkan instance, selects a compute-capable physical device, and sets up a logical
 * device with a dedicated compute queue and command pool. Buffer and image memory is managed via
 * the Vulkan Memory Allocator (VMA). An opt-in validation layer enables detailed API usage
 * checking during development.
 *
 * System property `kompute.backend.vulkan.dedicatedComputeQueue`: if set, prefers a compute queue
 * family without graphics capabilities, falling back to a combined family if none is found.
 *
 * System property `kompute.backend.vulkan.device`: if set, only physical devices whose name
 * contains this value (case-insensitive) are considered; initialization fails if none match.
 *
 * System property `kompute.backend.vulkan.validation`: if `true`, enables the Khronos validation
 * layer (`VK_LAYER_KHRONOS_validation`) and routes its messages to the logger; initialization fails
 * if the layer is not installed. Meant for development, the layer slows down every call.
 */
class VulkanBackend : AbstractBackend() {
    private lateinit var vulkanInstance: VulkanInstance
    private lateinit var vulkanPhysicalDevice: VulkanPhysicalDevice
    private lateinit var vulkanDevice: VulkanDevice
    private lateinit var vulkanCommandPool: VulkanCommandPool
    private lateinit var vulkanAllocator: VulkanAllocator
    private lateinit var vulkanContext: VulkanContext

    @InternalApi
    override fun type(): Type = Type.Vulkan
    override fun doInitialize() {
        vulkanInstance = VulkanInstance(System.getProperty(VALIDATION_PROPERTY).toBoolean())
        val dedicatedComputeQueue = System.getProperty(DEDICATED_COMPUTE_QUEUE_PROPERTY).toBoolean()
        val deviceName = System.getProperty(DEVICE_NAME_PROPERTY)
        vulkanPhysicalDevice = VulkanPhysicalDevice(dedicatedComputeQueue, deviceName, vulkanInstance)
        vulkanDevice = VulkanDevice(vulkanPhysicalDevice)
        vulkanCommandPool = VulkanCommandPool(vulkanDevice, vulkanPhysicalDevice)
        vulkanAllocator = VulkanAllocator(vulkanInstance, vulkanPhysicalDevice, vulkanDevice)
        vulkanContext = VulkanContext(
            vulkanDevice, vulkanAllocator, vulkanCommandPool, vulkanPhysicalDevice.limits(),
        )

        logger.info {
            "Vulkan Backend initialized with device: ${vulkanPhysicalDevice.name()}, " +
                    "apiVersion: ${vulkanPhysicalDevice.apiVersion()}, " +
                    "deviceLocalMemory: ${vulkanPhysicalDevice.deviceLocalMemory() / BYTES_PER_GB} GB, " +
                    "driverVersion: ${vulkanPhysicalDevice.driverVersion()}"
        }
    }

    @Suppress("TooGenericExceptionCaught") // cleans up on any failure and rethrows
    override fun compileSource(source: ShaderSource): CompiledShader {
        val vulkanShaderModule = VulkanShaderModule(source, vulkanDevice)
        val vulkanPipeline = try {
            VulkanPipeline(vulkanDevice, vulkanShaderModule)
        } catch (t: Throwable) {
            vulkanShaderModule.close()
            throw t
        }
        return VulkanCompiledShader(vulkanShaderModule, vulkanPipeline, vulkanContext)
    }

    override fun close() {
        if (::vulkanContext.isInitialized) {
            vulkanContext.close()
        }
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
        private const val DEDICATED_COMPUTE_QUEUE_PROPERTY = "kompute.backend.vulkan.dedicatedComputeQueue"
        private const val DEVICE_NAME_PROPERTY = "kompute.backend.vulkan.device"
        private const val VALIDATION_PROPERTY = "kompute.backend.vulkan.validation"
        private const val BYTES_PER_GB = 1_024 * 1_024 * 1_024
        private val logger = KotlinLogging.logger {}

        /**
         * The Vulkan API version this backend targets.
         */
        val API_VERSION = VK13.VK_API_VERSION_1_3
    }
}
