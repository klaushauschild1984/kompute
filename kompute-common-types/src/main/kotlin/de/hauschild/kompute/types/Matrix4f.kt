package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.FixedSize
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * @property columns the 4 column vectors, GLSL's own `mat4` storage order
 */
@GpuStruct
@Align(16)
data class Matrix4f(
    @GpuField @FixedSize(4) val columns: Array<Vector4f>,
) {
    /**
     * @return the matrix as a 16-element [FloatArray] in column-major order
     */
    fun toFloatArray(): FloatArray = FloatArray(16) { i -> columns[i / 4].toFloatArray()[i % 4] }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Matrix4f && columns.contentEquals(other.columns))

    override fun hashCode(): Int = columns.contentHashCode()

    companion object {
        /**
         * @param array a 16-element [FloatArray] in column-major order
         * @return the constructed [Matrix4f]
         */
        fun ofColumnMajor(array: FloatArray): Matrix4f {
            require(array.size == 16) { "Expected 16 elements, but was ${array.size}" }
            return Matrix4f(
                Array(4) { col ->
                    Vector4f(array[col * 4], array[col * 4 + 1], array[col * 4 + 2], array[col * 4 + 3])
                },
            )
        }

        /**
         * @param array a 16-element [FloatArray] in row-major order
         * @return the constructed [Matrix4f]
         */
        fun ofRowMajor(array: FloatArray): Matrix4f {
            require(array.size == 16) { "Expected 16 elements, but was ${array.size}" }
            return Matrix4f(
                Array(4) { col ->
                    Vector4f(array[col], array[4 + col], array[8 + col], array[12 + col])
                },
            )
        }

        /**
         * @param array a 16-element [FloatArray] in column-major order, GLSL's own `mat4` order
         * @return the constructed [Matrix4f]
         */
        fun of(array: FloatArray): Matrix4f = ofColumnMajor(array)
    }
}
