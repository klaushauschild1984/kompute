package de.hauschild.kompute.vulkan.data

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.vulkan.VulkanBuffer
import de.hauschild.kompute.vulkan.VulkanReadable

/**
 * Wraps a Vulkan shader storage buffer (SSBO) for a [StorageBuffer].
 *
 * @param T the data type — must be [IntArray], [LongArray], [FloatArray], [DoubleArray], or [ByteArray]
 * @param source the [StorageBuffer] configuration this buffer is based on
 */
class VulkanStorageBuffer<T : Any>(
    source: StorageBuffer<T>,
) : VulkanBuffer<StorageBuffer<T>>(source),
OutputCapable<T> by source,
VulkanReadable<T> {
    override fun bind() {
        TODO("Not yet implemented")
    }

    override fun read(): T {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}
