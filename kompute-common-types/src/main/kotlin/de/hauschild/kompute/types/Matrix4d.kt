package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.FixedSize
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * @property columns the 4 column vectors, GLSL's own `mat4` storage order
 */
@GpuStruct
@Align(32)
data class Matrix4d(
    @GpuField @FixedSize(4) val columns: Array<Vector4d>,
) {
    /**
     * @return the matrix as a 16-element [DoubleArray] in column-major order
     */
    fun toDoubleArray(): DoubleArray = DoubleArray(16) { i -> columns[i / 4].toDoubleArray()[i % 4] }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Matrix4d && columns.contentEquals(other.columns))

    override fun hashCode(): Int = columns.contentHashCode()

    companion object {
        /**
         * @param array a 16-element [DoubleArray] in column-major order
         * @return the constructed [Matrix4d]
         */
        fun ofColumnMajor(array: DoubleArray): Matrix4d {
            require(array.size == 16) { "Expected 16 elements, but was ${array.size}" }
            return Matrix4d(
                Array(4) { col ->
                    Vector4d(array[col * 4], array[col * 4 + 1], array[col * 4 + 2], array[col * 4 + 3])
                },
            )
        }

        /**
         * @param array a 16-element [DoubleArray] in row-major order
         * @return the constructed [Matrix4d]
         */
        fun ofRowMajor(array: DoubleArray): Matrix4d {
            require(array.size == 16) { "Expected 16 elements, but was ${array.size}" }
            return Matrix4d(
                Array(4) { col ->
                    Vector4d(array[col], array[4 + col], array[8 + col], array[12 + col])
                },
            )
        }

        /**
         * @param array a 16-element [DoubleArray] in column-major order, GLSL's own `mat4` order
         * @return the constructed [Matrix4d]
         */
        fun of(array: DoubleArray): Matrix4d = ofColumnMajor(array)
    }
}
