package de.hauschild.kompute.vulkan.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import java.nio.ByteBuffer
import kotlin.reflect.KClass

/**
 * Converts the primitive arrays supported by a [de.hauschild.kompute.core.data.StorageBuffer] to and
 * from the raw bytes of a Vulkan buffer.
 *
 * Supported types are [IntArray], [LongArray], [FloatArray], [DoubleArray] and [ByteArray]. All
 * [ByteBuffer]s passed in must use native byte order.
 */
internal object StorageBufferCodec {
    /**
     * Returns the number of bytes of a single element of [type].
     *
     * @param type the array type
     * @return the element size in bytes
     * @throws KomputeConfigurationException if [type] is not supported
     */
    fun elementSizeInBytes(type: KClass<*>): Int = when (type) {
        IntArray::class -> Int.SIZE_BYTES
        LongArray::class -> Long.SIZE_BYTES
        FloatArray::class -> Float.SIZE_BYTES
        DoubleArray::class -> Double.SIZE_BYTES
        ByteArray::class -> 1
        else -> unsupportedType(type)
    }

    /**
     * Returns the number of elements of [data].
     *
     * @param data a supported primitive array
     * @return the number of elements
     * @throws KomputeConfigurationException if the type of [data] is not supported
     */
    fun elementCount(data: Any): Int = when (data) {
        is IntArray -> data.size
        is LongArray -> data.size
        is FloatArray -> data.size
        is DoubleArray -> data.size
        is ByteArray -> data.size
        else -> unsupportedType(data::class)
    }

    /**
     * Writes [data] to the beginning of [target].
     *
     * @param data a supported primitive array
     * @param target the buffer to write to, in native byte order
     * @throws KomputeConfigurationException if the type of [data] is not supported
     */
    fun write(
        data: Any,
        target: ByteBuffer,
    ) {
        when (data) {
            is IntArray -> target.asIntBuffer().put(data)
            is LongArray -> target.asLongBuffer().put(data)
            is FloatArray -> target.asFloatBuffer().put(data)
            is DoubleArray -> target.asDoubleBuffer().put(data)
            is ByteArray -> target.put(data)
            else -> unsupportedType(data::class)
        }
    }

    /**
     * Reads [count] elements of [type] from the beginning of [bytes].
     *
     * @param type the array type
     * @param count the number of elements to read
     * @param bytes the buffer to read from, in native byte order
     * @return a new array of the given type with the read elements
     * @throws KomputeConfigurationException if [type] is not supported
     */
    fun read(
        type: KClass<*>,
        count: Int,
        bytes: ByteBuffer,
    ): Any = when (type) {
        IntArray::class -> IntArray(count).also { bytes.asIntBuffer().get(it) }
        LongArray::class -> LongArray(count).also { bytes.asLongBuffer().get(it) }
        FloatArray::class -> FloatArray(count).also { bytes.asFloatBuffer().get(it) }
        DoubleArray::class -> DoubleArray(count).also { bytes.asDoubleBuffer().get(it) }
        ByteArray::class -> ByteArray(count).also { bytes.get(it) }
        else -> unsupportedType(type)
    }

    private fun unsupportedType(type: KClass<*>): Nothing =
        throw KomputeConfigurationException("Unsupported StorageBuffer type: $type")
}
