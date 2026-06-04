package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.ShaderData.OutputCapable
import de.hauschild.kompute.core.ShaderData.StorageBuffer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL43

class OpenGLStorageBuffer(
    private val source: StorageBuffer,
) : AutoCloseable,
    OutputCapable by source {
    private var glHandle: Int = 0

    fun validate(maxBindings: Int) {
        require(source.index < maxBindings) {
            "StorageBuffer index ${source.index} exceeds maximum binding index ${maxBindings - 1}"
        }
    }

    fun bind() {
        val kind = if (isOutput()) "output" else "input"
        logger.debug {
            "Binding $kind buffer ${source.index}${source.outputName?.let {
                ":$it"
            } ?: ""} (${size()} elements)"
        }
        glHandle = GL43.glGenBuffers()
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, glHandle)
        if (isOutput()) {
            val sizeInBytes = source.size!! * Float.SIZE_BYTES
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, sizeInBytes.toLong(), GL43.GL_DYNAMIC_READ)
        } else {
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, source.data!!, GL43.GL_STATIC_DRAW)
        }
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, source.index, glHandle)
    }

    fun read(): FloatArray {
        logger.debug { "Reading buffer ${source.index}:${source.outputName}" }
        val buffer = FloatArray(size())
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, glHandle)
        GL43.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buffer)
        logger.trace {
            """
            Data:
            ${buffer.take(MAX_TRACE_ELEMENTS).joinToString(", ")}
            """.trimIndent()
        }
        return buffer
    }

    private fun size(): Int = source.data?.size ?: source.size!!

    override fun close() {
        if (glHandle == 0) return
        GL43.glDeleteBuffers(glHandle)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private const val MAX_TRACE_ELEMENTS = 100
    }
}
