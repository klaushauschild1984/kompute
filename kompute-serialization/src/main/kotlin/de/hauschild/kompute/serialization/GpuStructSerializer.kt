package de.hauschild.kompute.serialization

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Serializes [de.hauschild.kompute.serialization.annotation.GpuStruct] annotated classes. Do not use directly.
 *
 * @param size the size of the buffer to use
 */
class GpuStructSerializer(size: Int) {
    private val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)

    /**
     * Writes an integer to the buffer.
     *
     * @param value the integer to write
     */
    fun write(value: Int) {
        buffer.putInt(value)
    }

    /**
     * Writes a float to the buffer.
     *
     * @param value the float to write
     */
    fun write(value: Float) {
        buffer.putFloat(value)
    }

    /**
     * Writes a boolean to the buffer.
     *
     * @param value the boolean to write
     */
    fun write(value: Boolean) {
        buffer.putInt(if (value) 1 else 0)
    }

    /**
     * Writes a byte array to the buffer.
     *
     * @param value the byte array to write
     */
    fun write(value: ByteArray) {
        buffer.put(value)
    }

    /**
     * Skips the given number of bytes in the buffer.
     *
     * @param size the number of bytes to skip
     */
    fun skip(size: Int) {
        buffer.position(buffer.position() + size)
    }

    /**
     * Produces a byte array from the buffer.
     *
     * @return the byte array
     */
    fun toByteArray(): ByteArray = buffer.array()
}
