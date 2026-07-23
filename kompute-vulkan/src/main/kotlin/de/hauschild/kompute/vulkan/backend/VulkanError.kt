/**
 * Bundles a readable-name lookup for Vulkan `VkResult` values.
 */

package de.hauschild.kompute.vulkan.backend

import org.lwjgl.vulkan.VK10

private val VK_RESULT_NAMES = mapOf(
    VK10.VK_SUCCESS to "VK_SUCCESS",
    VK10.VK_NOT_READY to "VK_NOT_READY",
    VK10.VK_TIMEOUT to "VK_TIMEOUT",
    VK10.VK_EVENT_SET to "VK_EVENT_SET",
    VK10.VK_EVENT_RESET to "VK_EVENT_RESET",
    VK10.VK_INCOMPLETE to "VK_INCOMPLETE",
    VK10.VK_ERROR_OUT_OF_HOST_MEMORY to "VK_ERROR_OUT_OF_HOST_MEMORY",
    VK10.VK_ERROR_OUT_OF_DEVICE_MEMORY to "VK_ERROR_OUT_OF_DEVICE_MEMORY",
    VK10.VK_ERROR_INITIALIZATION_FAILED to "VK_ERROR_INITIALIZATION_FAILED",
    VK10.VK_ERROR_DEVICE_LOST to "VK_ERROR_DEVICE_LOST",
    VK10.VK_ERROR_MEMORY_MAP_FAILED to "VK_ERROR_MEMORY_MAP_FAILED",
    VK10.VK_ERROR_LAYER_NOT_PRESENT to "VK_ERROR_LAYER_NOT_PRESENT",
    VK10.VK_ERROR_EXTENSION_NOT_PRESENT to "VK_ERROR_EXTENSION_NOT_PRESENT",
    VK10.VK_ERROR_FEATURE_NOT_PRESENT to "VK_ERROR_FEATURE_NOT_PRESENT",
    VK10.VK_ERROR_INCOMPATIBLE_DRIVER to "VK_ERROR_INCOMPATIBLE_DRIVER",
    VK10.VK_ERROR_TOO_MANY_OBJECTS to "VK_ERROR_TOO_MANY_OBJECTS",
    VK10.VK_ERROR_FORMAT_NOT_SUPPORTED to "VK_ERROR_FORMAT_NOT_SUPPORTED",
    VK10.VK_ERROR_FRAGMENTED_POOL to "VK_ERROR_FRAGMENTED_POOL",
)

/**
 * Maps a raw Vulkan `VkResult` value to a readable name, e.g. `-9` -> `VK_ERROR_INCOMPATIBLE_DRIVER (-9)`.
 *
 * There is no Vulkan or LWJGL API for this - `VkResult` is a plain `Int` in both. Only the core
 * result codes are covered here; unrecognized values fall back to their raw number.
 *
 * @param result the raw `VkResult` value returned by a Vulkan call
 * @return the constant name paired with the raw value
 */
fun vkResultName(result: Int): String {
    val name = VK_RESULT_NAMES[result] ?: "UNKNOWN_VK_RESULT"
    return "$name ($result)"
}
