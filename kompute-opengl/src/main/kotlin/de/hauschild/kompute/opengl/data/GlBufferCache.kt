package de.hauschild.kompute.opengl.data

import org.lwjgl.opengl.GL43
import java.lang.ref.Cleaner
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Manages GL buffer object lifetimes for a specific source type [K].
 *
 * Handles are allocated once per [K] instance and cached in a [WeakHashMap].
 * When the [K] instance becomes unreachable, the associated GL buffer is
 * scheduled for deletion via a shared [Cleaner] and flushed on the next [flushDeletions] call.
 *
 * **Thread safety**: [flushDeletions] and [getOrAllocate] are not thread-safe and must be called
 * on the OpenGL thread. The [Cleaner] safely enqueues deletions from any thread via
 * [ConcurrentLinkedQueue]; only the dequeue in [flushDeletions] requires the GL thread.
 *
 * **Data mutation**: The cache key is the [K] instance identity. If the [K] instance's data is
 * mutated after the first [getOrAllocate] call, the GPU-side buffer will not be updated — the
 * cache returns the existing handle without re-uploading.
 *
 * @param K the source object type used as cache key
 */
internal class GlBufferCache<K : Any> {
    private val handles = WeakHashMap<K, Int>()
    private val pendingDeletions = ConcurrentLinkedQueue<Int>()

    /**
     * Deletes all GL buffers whose source objects have become unreachable.
     *
     * Must be called on the OpenGL thread.
     */
    fun flushDeletions() {
        generateSequence { pendingDeletions.poll() }.forEach { GL43.glDeleteBuffers(it) }
    }

    /**
     * Returns the cached GL handle for [key], or allocates a new one.
     *
     * @param key the source object used as cache key
     * @return the GL handle (first) and whether it was freshly allocated (second)
     */
    fun getOrAllocate(key: K): Pair<Int, Boolean> {
        handles[key]?.let { return it to false }
        val handle = GL43.glGenBuffers()
        handles[key] = handle
        cleaner.register(key) { pendingDeletions.add(handle) }
        return handle to true
    }

    companion object {
        private val cleaner = Cleaner.create()
    }
}
