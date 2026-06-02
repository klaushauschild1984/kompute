package de.hauschild.kompute.opengl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL43

class Buffer(
    private val index: Int,
    private val data: FloatArray,
    private val name: String? = null,
) : AutoCloseable {
    private var bufferId: Int = 0

    fun bindAsInput() {
        bind("input") {
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, data, GL43.GL_STATIC_DRAW)
        }
        logger.trace {
            """
            Data:
            ${data.contentToString()}
            """.trimIndent()
        }
    }

    fun bindAsOutput() {
        bind("output") {
            val sizeInBytes = data.size * Float.SIZE_BYTES
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, sizeInBytes.toLong(), GL43.GL_DYNAMIC_READ)
        }
    }

    private fun bind(
        kind: String,
        binder: () -> Unit,
    ) {
        logger.debug { "Binding $kind buffer $index${name?.let { ":$it" } ?: ""} (${data.size} elements)" }
        bufferId = GL43.glGenBuffers()
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, bufferId)
        binder()
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, index, bufferId)
    }

    fun read(): FloatArray {
        logger.debug { "Reading buffer $index:$name" }
        val buffer = FloatArray(data.size)
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, bufferId)
        GL43.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buffer)
        logger.trace {
            """
            Data:
            ${buffer.contentToString()}
            """.trimIndent()
        }
        return buffer
    }

    override fun close() {
        if (bufferId == 0) return
        GL43.glDeleteBuffers(bufferId)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
