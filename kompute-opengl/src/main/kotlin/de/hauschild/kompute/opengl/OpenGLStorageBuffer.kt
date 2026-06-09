package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.lwjgl.opengl.GL43
import org.lwjgl.system.MemoryUtil

import java.nio.ByteBuffer

/**
 * Wraps an OpenGL shader storage buffer object (SSBO) for a [StorageBuffer].
 *
 * @param T the data type — must be [IntArray], [FloatArray], [DoubleArray], or [ByteArray]
 * @param source the [StorageBuffer] configuration this buffer is based on
 */
class OpenGLStorageBuffer<T : Any>(
    source: StorageBuffer<T>,
) : Buffer<StorageBuffer<T>>(source),
OutputCapable<T> by source,
Readable<T> {
    override val barrierBit: Int = GL43.GL_SHADER_STORAGE_BARRIER_BIT

    override fun bind() {
        glHandle = GL43.glGenBuffers()
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, glHandle)
        when (val mode = source.mode()) {
            is StorageBuffer.Mode.Input -> uploadData(mode.data, GL43.GL_STATIC_DRAW)
            is StorageBuffer.Mode.Output -> GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER,
                (mode.size * elementSizeInBytes()).toLong(), GL43.GL_DYNAMIC_READ)
            is StorageBuffer.Mode.ReadWrite -> uploadData(mode.data, GL43.GL_DYNAMIC_COPY)
        }
        GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, source.index, glHandle)
    }

    private fun uploadData(data: T, usage: Int) {
        when (data) {
            is IntArray -> GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, data, usage)
            is FloatArray -> GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, data, usage)
            is DoubleArray -> GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, data, usage)
            is ByteArray -> GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, ByteBuffer.wrap(data), usage)
            else -> throw KomputeConfigurationException("Unsupported StorageBuffer type: ${source.type}")
        }
    }

    private fun elementSizeInBytes(): Int =
        when (source.type) {
            IntArray::class -> Int.SIZE_BYTES
            FloatArray::class -> Float.SIZE_BYTES
            DoubleArray::class -> Double.SIZE_BYTES
            ByteArray::class -> 1
            else -> throw KomputeConfigurationException("Unsupported StorageBuffer type: ${source.type}")
        }

    @Suppress("UNCHECKED_CAST")
    override fun read(): T {
        val elementCount = when (val mode = source.mode()) {
            is StorageBuffer.Mode.Output -> mode.size
            is StorageBuffer.Mode.ReadWrite -> when (val d = mode.data) {
                is IntArray -> d.size
                is FloatArray -> d.size
                is DoubleArray -> d.size
                is ByteArray -> d.size
                else -> throw KomputeConfigurationException("Unsupported StorageBuffer type: ${source.type}")
            }
            is StorageBuffer.Mode.Input -> error("Cannot read an input-only buffer")
        }
        val buffer: T =
            when (source.type) {
                IntArray::class -> IntArray(elementCount)
                FloatArray::class -> FloatArray(elementCount)
                DoubleArray::class -> DoubleArray(elementCount)
                ByteArray::class -> ByteArray(elementCount)
                else -> throw KomputeConfigurationException("Unsupported StorageBuffer type: ${source.type}")
            } as T
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, glHandle)
        when (buffer) {
            is IntArray -> GL43.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buffer)
            is FloatArray -> GL43.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buffer)
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
}
