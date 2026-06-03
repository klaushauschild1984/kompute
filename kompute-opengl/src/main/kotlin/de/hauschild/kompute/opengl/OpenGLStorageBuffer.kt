package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.ShaderData.StorageBuffer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL43

class OpenGLStorageBuffer(
    private val storageBuffer: StorageBuffer,
) : AutoCloseable {
    private var bufferId: Int = 0

    fun isOutput(): Boolean = storageBuffer.outputName != null

    fun outputName(): String = storageBuffer.outputName!!

    fun bind() {
        val kind = if (isOutput()) "output" else "input"
        logger.debug {
            "Binding $kind buffer ${storageBuffer.index}${storageBuffer.outputName?.let {
                ":$it"
            } ?: ""} (${size()} elements)"
        }
        bufferId = GL43.glGenBuffers()
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, bufferId)
        if (isOutput()) {
            val sizeInBytes = storageBuffer.size!! * Float.SIZE_BYTES
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, sizeInBytes.toLong(), GL43.GL_DYNAMIC_READ)
        } else {
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, storageBuffer.data!!, GL43.GL_STATIC_DRAW)
        }
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, storageBuffer.index, bufferId)
    }

    fun read(): FloatArray {
        logger.debug { "Reading buffer ${storageBuffer.index}:${storageBuffer.outputName}" }
        val buffer = FloatArray(size())
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, bufferId)
        GL43.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buffer)
        logger.trace {
            """
            Data:
            ${buffer.take(MAX_TRACE_ELEMENTS).joinToString(", ")}
            """.trimIndent()
        }
        return buffer
    }

    private fun size(): Int = storageBuffer.data?.size ?: storageBuffer.size!!

    override fun close() {
        if (bufferId == 0) return
        GL43.glDeleteBuffers(bufferId)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private const val MAX_TRACE_ELEMENTS = 100
    }
}
