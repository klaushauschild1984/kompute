/**
 * Pure device-ranking heuristics for [VulkanPhysicalDevice] - no Vulkan calls, fully unit-testable.
 */

package de.hauschild.kompute.vulkan.backend

import org.lwjgl.vulkan.VK10.VK_MEMORY_HEAP_DEVICE_LOCAL_BIT
import org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_CPU
import org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU
import org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU
import org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU
import org.lwjgl.vulkan.VK10.VK_QUEUE_COMPUTE_BIT
import org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT

/**
 * Sums the size of all device-local heaps.
 *
 * @param heaps the memory heaps to sum.
 * @return the total device-local memory in bytes.
 */
internal fun sumDeviceLocalMemory(heaps: List<HeapInfo>): Long =
    heaps.filter { it.flags and VK_MEMORY_HEAP_DEVICE_LOCAL_BIT != 0 }
        .sumOf { it.size }

/**
 * Ranks a `VkPhysicalDeviceType` by suitability for compute: discrete GPUs first, followed by
 * integrated and virtual GPUs, then CPU-based software renderers. Unrecognized values rank
 * lowest.
 *
 * @param type the raw `VkPhysicalDeviceType` value.
 * @return a rank where a higher value is more preferred.
 */
internal fun deviceTypeRank(type: Int): Int = when (type) {
    VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> 4
    VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU -> 3
    VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU -> 2
    VK_PHYSICAL_DEVICE_TYPE_CPU -> 1
    else -> 0
}

/**
 * Checks whether a queue family's flags satisfy the compute requirement.
 *
 * @param queueFlags the queue family's `VkQueueFlagBits` bitmask.
 * @param dedicatedComputeQueue if `true`, only a compute-capable family without graphics
 * capabilities matches; if `false`, any compute-capable family matches.
 * @return `true` if the queue family satisfies the requirement.
 */
internal fun isComputeRequirementSatisfied(queueFlags: Int, dedicatedComputeQueue: Boolean): Boolean =
    if (dedicatedComputeQueue) {
        queueFlags and VK_QUEUE_COMPUTE_BIT != 0 && queueFlags and VK_QUEUE_GRAPHICS_BIT == 0
    } else {
        queueFlags and VK_QUEUE_COMPUTE_BIT != 0
    }

/**
 * Checks whether a physical device's name matches a filter - a case-insensitive substring match,
 * since vendor-reported device names often carry driver-/version-specific suffixes that would
 * make an exact match brittle.
 *
 * @param deviceName the physical device's reported name.
 * @param filter the filter to match against.
 * @return `true` if [deviceName] contains [filter], ignoring case.
 */
internal fun isDeviceNameMatch(deviceName: String, filter: String): Boolean =
    deviceName.contains(filter, ignoreCase = true)
