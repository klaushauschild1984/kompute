package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.UniformBuffer
import org.lwjgl.opengl.GL43
import org.lwjgl.system.MemoryUtil

/**
 * Wraps an OpenGL uniform buffer object (UBO) for a [UniformBuffer].
 *
 * @param source the [UniformBuffer] configuration this buffer is based on
 */
class OpenGLUniformBuffer(
    source: UniformBuffer
) : OpenGLBuffer<UniformBuffer>(source) {
    override fun bind() {
        val data = source.data!!
        glHandle = GL43.glGenBuffers()
        GL43.glBindBuffer(GL43.GL_UNIFORM_BUFFER, glHandle)
        MemoryUtil.memAlloc(data.size).useAndFree { bytes ->
            bytes.put(data)
            bytes.flip()
            GL43.glBufferData(GL43.GL_UNIFORM_BUFFER, bytes, GL43.GL_STATIC_DRAW)
        }
        GL43.glBindBufferBase(GL43.GL_UNIFORM_BUFFER, source.index, glHandle)
    }
}
