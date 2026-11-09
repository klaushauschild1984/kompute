package de.hauschild.kompute.vulkan.backend

/**
 * A single Vulkan memory heap's relevant properties, decoupled from the native
 * [org.lwjgl.vulkan.VkMemoryHeap] struct so this data can be constructed in tests without a real
 * Vulkan context.
 *
 * @property flags the heap's `VkMemoryHeapFlagBits` bitmask.
 * @property size the heap size in bytes.
 */
internal data class HeapInfo(val flags: Int, val size: Long)
