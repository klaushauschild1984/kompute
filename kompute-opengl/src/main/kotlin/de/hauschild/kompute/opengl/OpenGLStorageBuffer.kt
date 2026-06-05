package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.KomputeConfigurationException
import de.hauschild.kompute.core.ShaderData.OutputCapable
import de.hauschild.kompute.core.ShaderData.StorageBuffer
import de.hauschild.kompute.core.requireConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL43
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * Wraps an OpenGL shader storage buffer object (SSBO) for a [StorageBuffer].
 *
 * Handles GPU buffer allocation, data upload, dispatch, and readback.
 * Must be used within a `use` block — releases the OpenGL buffer handle on close.
 *
 * @param T the data type — must be [FloatArray], [IntArray], [DoubleArray], or [ByteArray]
 * @property source the [StorageBuffer] configuration this buffer is based on
 */
class OpenGLStorageBuffer<T : Any>(
    val source: StorageBuffer<T>,
) : AutoCloseable,
OutputCapable<T> by source {
    private var glHandle: Int = 0

    /**
     * Validates that the buffer's binding index is within the GPU's supported range.
     *
     * @param maxBindings the maximum number of shader storage buffer bindings supported by the GPU
     * @throws de.hauschild.kompute.core.KomputeConfigurationException if the index exceeds the limit
     */
    fun validate(maxBindings: Int) {
        requireConfiguration(source.index < maxBindings) {
            "StorageBuffer index ${source.index} exceeds maximum binding index ${maxBindings - 1}"
        }
    }

    /**
     * Allocates the GPU buffer and uploads input data or reserves output memory.
     *
     * @throws KomputeConfigurationException if the data type is not supported
     */
    fun bind() {
        val kind = if (isOutput) "output" else "input"
        logger.debug {
            "Binding $kind buffer ${source.index}"
        }
        glHandle = GL43.glGenBuffers()
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, glHandle)
        if (isOutput) {
            val sizeInBytes = source.size!! * elementSizeInBytes()
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, sizeInBytes.toLong(), GL43.GL_DYNAMIC_READ)
        } else {
            when (val data = source.data!!) {
                is FloatArray -> GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, data, GL43.GL_STATIC_DRAW)
                is IntArray -> GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, data, GL43.GL_STATIC_DRAW)
                is DoubleArray -> GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, data, GL43.GL_STATIC_DRAW)
                is ByteArray ->
                    GL43.glBufferData(
                        GL43.GL_SHADER_STORAGE_BUFFER,
                        ByteBuffer.wrap(data),
                        GL43.GL_STATIC_DRAW,
                    )
                else -> throw KomputeConfigurationException("Unsupported StorageBuffer type: ${source.type}")
            }
        }
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, source.index, glHandle)
    }

    private fun elementSizeInBytes(): Int =
        when (source.type) {
            FloatArray::class -> Float.SIZE_BYTES
            IntArray::class -> Int.SIZE_BYTES
            DoubleArray::class -> Double.SIZE_BYTES
            ByteArray::class -> 1
            else -> throw KomputeConfigurationException("Unsupported StorageBuffer type: ${source.type}")
        }

    /**
     * Reads the buffer contents from the GPU back to host memory.
     *
     * @return the data read from the GPU buffer
     * @throws KomputeConfigurationException if the data type is not supported
     */
    @Suppress("UNCHECKED_CAST")
    fun read(): T {
        logger.debug { "Reading buffer ${source.index}" }
        val buffer: T =
            when (source.type) {
                FloatArray::class -> FloatArray(source.size!!)
                IntArray::class -> IntArray(source.size!!)
                DoubleArray::class -> DoubleArray(source.size!!)
                ByteArray::class -> ByteArray(source.size!!)
                else -> throw KomputeConfigurationException("Unsupported StorageBuffer type: ${source.type}")
            } as T
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, glHandle)
        when (buffer) {
            is FloatArray -> GL43.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buffer)
            is IntArray -> GL43.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buffer)
            is DoubleArray -> GL43.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buffer)
            is ByteArray -> {
                val direct = MemoryUtil.memAlloc(buffer.size)
                try {
                    GL43.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, direct)
                    direct.rewind()
                    direct.get(buffer)
                } finally {
                    MemoryUtil.memFree(direct)
                }
            }
        }
        return buffer
    }

    override fun close() {
        if (glHandle == 0) {
            return
        }
        GL43.glDeleteBuffers(glHandle)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
