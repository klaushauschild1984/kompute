package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.BuildInfo
import de.hauschild.kompute.core.exception.requireBackendInitialization
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.EXTDebugUtils
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.VK_MAKE_API_VERSION
import org.lwjgl.vulkan.VK10.vkCreateInstance
import org.lwjgl.vulkan.VK10.vkDestroyInstance
import org.lwjgl.vulkan.VK13
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo
import org.lwjgl.vulkan.VkLayerProperties
import kotlin.use

/**
 * Wrapper around [VkInstance] handling initialization and closing.
 *
 * @param validation whether to enable the Khronos validation layer and route its messages to the logger
 * @throws de.hauschild.kompute.core.exception.KomputeBackendInitializationException if [validation] is requested
 * but the validation layer is not installed, or if the instance cannot be created
 */
class VulkanInstance(
    private val validation: Boolean = false,
) : AutoCloseable {
    private var debugMessenger: Long = VK10.VK_NULL_HANDLE
    private var debugCallback: VkDebugUtilsMessengerCallbackEXT? = null

    /**
     * The raw [VkInstance].
     */
    val instance: VkInstance = createInstance()

    init {
        if (validation) {
            createDebugMessenger()
        }
    }

    private fun createInstance(): VkInstance {
        MemoryStack.stackPush().use { stack ->
            if (validation) {
                requireBackendInitialization(isLayerAvailable(VALIDATION_LAYER, stack)) {
                    "Vulkan validation was requested, but $VALIDATION_LAYER is not installed"
                }
            }
            val applicationInfo = VkApplicationInfo.calloc(stack)
                .sType(VK13.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(stack.UTF8("kompute"))
                .applicationVersion(packedVersion())
                .pEngineName(stack.UTF8("kompute"))
                .engineVersion(packedVersion())
                .apiVersion(VulkanBackend.API_VERSION)

            val instanceCreateInfo = VkInstanceCreateInfo.calloc(stack)
                .sType(VK13.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(applicationInfo)
            if (validation) {
                val debugUtils = stack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
                instanceCreateInfo
                    .ppEnabledLayerNames(stack.pointers(stack.UTF8(VALIDATION_LAYER)))
                    .ppEnabledExtensionNames(stack.pointers(debugUtils))
            }

            val instancePointer = stack.mallocPointer(1)
            val result = vkCreateInstance(instanceCreateInfo, null, instancePointer)
            requireBackendInitialization(result == VK13.VK_SUCCESS) {
                "Failed to create Vulkan instance: ${vkResultName(result)}"
            }

            return VkInstance(instancePointer.get(0), instanceCreateInfo)
        }
    }

    private fun isLayerAvailable(
        layerName: String,
        stack: MemoryStack,
    ): Boolean {
        val layerCount = stack.mallocInt(1)
        VK10.vkEnumerateInstanceLayerProperties(layerCount, null)
        val layers = VkLayerProperties.malloc(layerCount.get(0), stack)
        VK10.vkEnumerateInstanceLayerProperties(layerCount, layers)
        return layers.any { it.layerNameString() == layerName }
    }

    private fun createDebugMessenger() {
        val callback = VkDebugUtilsMessengerCallbackEXT.create { severity, _, callbackData, _ ->
            logValidationMessage(severity, VkDebugUtilsMessengerCallbackDataEXT.create(callbackData).pMessageString())
            VK10.VK_FALSE
        }
        debugCallback = callback
        MemoryStack.stackPush().use { stack ->
            val createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                .sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(MESSAGE_SEVERITIES)
                .messageType(MESSAGE_TYPES)
                .pfnUserCallback(callback)
            val messengerPointer = stack.mallocLong(1)
            val result = EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(instance, createInfo, null, messengerPointer)
            requireBackendInitialization(result == VK13.VK_SUCCESS) {
                "Failed to create Vulkan debug messenger: ${vkResultName(result)}"
            }
            debugMessenger = messengerPointer.get(0)
        }
    }

    private fun logValidationMessage(
        severity: Int,
        message: String?,
    ) {
        when {
            severity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT != 0 -> {
                VulkanValidationErrors.global.record(message.orEmpty())
                logger.error { "Vulkan validation: $message" }
            }
            severity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT != 0 ->
                logger.warn { "Vulkan validation: $message" }
            else -> logger.debug { "Vulkan validation: $message" }
        }
    }

    private fun packedVersion(): Int {
        val (major, minor, patch) = BuildInfo.VERSION
            .substringBefore('-')
            .split('.')
            .map { it.toInt() }
        return VK_MAKE_API_VERSION(0, major, minor, patch)
    }

    override fun close() {
        if (debugMessenger != VK10.VK_NULL_HANDLE) {
            EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null)
            debugMessenger = VK10.VK_NULL_HANDLE
        }
        debugCallback?.free()
        debugCallback = null
        vkDestroyInstance(instance, null)
    }

    companion object {
        private const val VALIDATION_LAYER = "VK_LAYER_KHRONOS_validation"
        private const val MESSAGE_SEVERITIES =
            VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
        private const val GENERAL_AND_VALIDATION =
            VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
        private const val MESSAGE_TYPES = GENERAL_AND_VALIDATION or VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
        private val logger = KotlinLogging.logger {}
    }
}
