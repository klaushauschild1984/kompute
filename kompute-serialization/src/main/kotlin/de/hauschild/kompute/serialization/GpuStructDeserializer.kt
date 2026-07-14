package de.hauschild.kompute.serialization

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Deserializes [de.hauschild.kompute.serialization.annotation.GpuStruct] annotated classes. Do not use directly.
 *
 * Reads are addressed by absolute byte offset rather than a sequential cursor, since the generated
 * `fromByteArray()` extension already knows every field's offset at KSP-compile-time.
 *
 * @param bytes the serialized struct to read from
 */
class GpuStructDeserializer(bytes: ByteArray) {
    private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

    /**
     * Reads an integer at the given offset.
     *
     * @param offset the byte offset to read from
     * @return the integer read
     */
    fun readInt(offset: Int): Int = buffer.getInt(offset)

    /**
     * Reads a float at the given offset.
     *
     * @param offset the byte offset to read from
     * @return the float read
     */
    fun readFloat(offset: Int): Float = buffer.getFloat(offset)

    /**
     * Reads a boolean at the given offset.
     *
     * @param offset the byte offset to read from
     * @return the boolean read
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun readBoolean(offset: Int): Boolean = buffer.getInt(offset) != 0
}
