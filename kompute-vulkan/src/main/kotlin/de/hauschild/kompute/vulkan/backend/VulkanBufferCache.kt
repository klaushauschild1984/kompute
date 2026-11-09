package de.hauschild.kompute.vulkan.backend

import java.lang.ref.Cleaner
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Keeps device-local buffers alive across dispatches, one buffer per shader data instance.
 *
 * A buffer is allocated the first time its key is seen and then reused, so GPU-side state (e.g.
 * particle positions) survives from dispatch to dispatch. This mirrors the OpenGL backend's
 * `GlBufferCache`.
 *
 * Buffers are released when
 * - the key becomes unreachable: a shared [Cleaner] queues the buffer, it is destroyed on the next
 *   [getOrAllocate] or [close] call, or
 * - the cache is [close]d, which happens when the backend is closed.
 *
 * **Data mutation**: the cache key is the identity of the key object. If its data is mutated after
 * the first [getOrAllocate] call, the buffer is not updated and the caller does not upload again.
 * Only a changed size replaces the buffer.
 *
 * @param allocator the [VulkanAllocator] the buffers are allocated from
 */
internal class VulkanBufferCache(
    private val allocator: VulkanAllocator,
) : AutoCloseable {
    private val buffers = WeakHashMap<Any, VulkanAllocatedBuffer>()
    private val pendingDeletions = ConcurrentLinkedQueue<VulkanAllocatedBuffer>()
    private var closed = false

    /**
     * Returns the cached buffer for [key], or allocates a new one.
     *
     * A cached buffer of a different size is released and replaced.
     *
     * @param key the shader data instance the buffer belongs to
     * @param size the required buffer size in bytes
     * @param usage the `VkBufferUsageFlags` of a newly allocated buffer
     * @return the buffer (first) and whether it is new and still has to be filled (second)
     * @throws IllegalStateException if the cache is already closed
     */
    @Synchronized
    fun getOrAllocate(
        key: Any,
        size: Long,
        usage: Int,
    ): Pair<VulkanAllocatedBuffer, Boolean> {
        check(!closed) { "The buffer cache is closed" }
        flushDeletions()
        val existing = buffers[key]
        if (existing != null && existing.size == size) {
            return existing to false
        }
        existing?.close()
        val buffer = allocator.createBuffer(size, usage, VulkanMemoryKind.DEVICE)
        buffers[key] = buffer
        cleaner.register(key) { pendingDeletions.add(buffer) }
        return buffer to true
    }

    /**
     * Releases the buffers of all keys that have become unreachable.
     */
    @Synchronized
    fun flushDeletions() {
        generateSequence { pendingDeletions.poll() }.forEach { it.close() }
    }

    @Synchronized
    override fun close() {
        closed = true
        flushDeletions()
        buffers.values.forEach { it.close() }
        buffers.clear()
    }

    companion object {
        private val cleaner = Cleaner.create()
    }
}
