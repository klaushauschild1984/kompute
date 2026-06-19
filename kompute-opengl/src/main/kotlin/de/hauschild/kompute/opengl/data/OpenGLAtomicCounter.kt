package de.hauschild.kompute.opengl.data

import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.exception.requireBackendInitialization
import de.hauschild.kompute.opengl.Buffer
import de.hauschild.kompute.opengl.Readable
import org.lwjgl.opengl.GL42
import org.lwjgl.opengl.GL43

/**
 * Wraps an OpenGL shader atomic counter for a [AtomicCounter].
 *
 * GL buffer lifetime is managed by [GlBufferCache]: allocated on first [bind], deleted when
 * the source becomes unreachable. The counter is reset to [AtomicCounter.start] on every [bind],
 * preserving per-dispatch reset semantics. [close] is intentionally a no-op.
 *
 * @param source the [AtomicCounter] configuration this buffer is based on
 */
class OpenGLAtomicCounter(
    source: AtomicCounter
): Buffer<AtomicCounter>(source),
OutputCapable<Int> by source,
Readable<Int> {
    override val barrierBit: Int = GL42.GL_ATOMIC_COUNTER_BARRIER_BIT

    override fun bind() {
        cache.flushDeletions()

        val (handle, isNew) = cache.getOrAllocate(source)
        glHandle = handle
        GL43.glBindBuffer(GL42.GL_ATOMIC_COUNTER_BUFFER, glHandle)
        if (isNew) {
            GL43.glBufferData(GL42.GL_ATOMIC_COUNTER_BUFFER, intArrayOf(source.start), GL43.GL_DYNAMIC_COPY)
        } else {
            GL43.glBufferSubData(GL42.GL_ATOMIC_COUNTER_BUFFER, 0L, intArrayOf(source.start))
        }
        GL43.glBindBufferBase(GL42.GL_ATOMIC_COUNTER_BUFFER, source.index, glHandle)
    }

    /**
     * Reads the current counter value back from the GPU buffer.
     *
     * @return the counter value after shader execution
     */
    override fun read(): Int {
        requireBackendInitialization(glHandle != 0) { "bind() must be called before read()" }
        val result = IntArray(1)
        GL43.glBindBuffer(GL42.GL_ATOMIC_COUNTER_BUFFER, glHandle)
        GL43.glGetBufferSubData(GL42.GL_ATOMIC_COUNTER_BUFFER, 0L, result)
        return result[0]
    }

    override fun close() {
        // GL buffer lifetime is managed by Cleaner — intentional no-op
    }

    companion object {
        private val cache = GlBufferCache<AtomicCounter>()
    }
}
