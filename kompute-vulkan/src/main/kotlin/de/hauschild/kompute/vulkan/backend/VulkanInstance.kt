package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.BuildInfo
import de.hauschild.kompute.core.exception.requireBackendInitialization
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.Platform
import org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR
import org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.VK_MAKE_API_VERSION
import org.lwjgl.vulkan.VK10.vkCreateInstance
import org.lwjgl.vulkan.VK10.vkDestroyInstance
import org.lwjgl.vulkan.VK13
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo
import kotlin.use

/**
 * Wrapper around [VkInstance] handling initialization and closing.
 */
class VulkanInstance : AutoCloseable {
    /**
     * The raw [VkInstance].
     */
    val instance: VkInstance = createInstance()

    private fun createInstance(): VkInstance {
        MemoryStack.stackPush().use { stack ->
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

            if (Platform.get() == Platform.MACOSX) {
                // MoltenVK is a portability implementation, not a fully conformant Vulkan driver;
                // the loader refuses to enumerate it unless this extension/flag is explicitly set.
                instanceCreateInfo
                    .flags(VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR)
                    .ppEnabledExtensionNames(stack.pointers(stack.UTF8(VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME)))
            }

            val instancePointer = stack.mallocPointer(1)
            val result = vkCreateInstance(instanceCreateInfo, null, instancePointer)
            requireBackendInitialization(result == VK13.VK_SUCCESS) {
                "Failed to create Vulkan instance: ${vkResultName(result)}"
            }

            return VkInstance(instancePointer.get(0), instanceCreateInfo)
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
        vkDestroyInstance(instance, null)
    }
}
