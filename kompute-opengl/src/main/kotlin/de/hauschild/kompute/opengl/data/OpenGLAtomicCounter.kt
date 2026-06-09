package de.hauschild.kompute.opengl.data

import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.opengl.Buffer
import de.hauschild.kompute.opengl.Readable
import org.lwjgl.opengl.GL42
import org.lwjgl.opengl.GL43

/**
 * Wraps an OpenGL shader atomic counter for a [AtomicCounter].
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
        glHandle = GL43.glGenBuffers()
        GL43.glBindBuffer(GL42.GL_ATOMIC_COUNTER_BUFFER, glHandle)
        GL43.glBufferData(GL42.GL_ATOMIC_COUNTER_BUFFER, intArrayOf(source.start), GL43.GL_DYNAMIC_COPY)
        GL43.glBindBufferBase(GL42.GL_ATOMIC_COUNTER_BUFFER, source.index, glHandle)
    }

    /**
     * Reads the current counter value back from the GPU buffer.
     *
     * @return the counter value after shader execution
     */
    override fun read(): Int {
        val result = IntArray(1)
        GL43.glBindBuffer(GL42.GL_ATOMIC_COUNTER_BUFFER, glHandle)
        GL43.glGetBufferSubData(GL42.GL_ATOMIC_COUNTER_BUFFER, 0L, result)
        return result[0]
    }
}
