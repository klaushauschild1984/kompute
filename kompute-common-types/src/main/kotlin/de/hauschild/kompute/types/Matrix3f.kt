package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.FixedSize
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * Matches GLSL's `mat3`, stored column-major exactly as GLSL does.
 *
 * ```glsl
 * layout(std140, binding = 0) uniform Params {
 *     mat3 value;
 * };
 * ```
 *
 * Use [ofColumnMajor] (or [of], its alias) when the source data is already column-major, e.g.
 * copied straight out of a shader or another GLSL-facing library. Use [ofRowMajor] when building
 * from row-major data, the layout most math textbooks and some other engines use.
 *
 * @property columns the 3 column vectors, GLSL's own `mat3` storage order
 */
@GpuStruct
@Align(16)
data class Matrix3f(
    @GpuField @FixedSize(3) val columns: Array<Vector3f>,
) {
    /**
     * @return the matrix as a 9-element [FloatArray] in column-major order
     */
    fun toFloatArray(): FloatArray = FloatArray(9) { i -> columns[i / 3].toFloatArray()[i % 3] }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Matrix3f && columns.contentEquals(other.columns))

    override fun hashCode(): Int = columns.contentHashCode()

    companion object {
        /**
         * @param array a 9-element [FloatArray] in column-major order
         * @return the constructed [Matrix3f]
         */
        fun ofColumnMajor(array: FloatArray): Matrix3f {
            require(array.size == 9) { "Expected 9 elements, but was ${array.size}" }
            return Matrix3f(
                Array(3) { col ->
                    Vector3f(array[col * 3], array[col * 3 + 1], array[col * 3 + 2])
                },
            )
        }

        /**
         * @param array a 9-element [FloatArray] in row-major order
         * @return the constructed [Matrix3f]
         */
        fun ofRowMajor(array: FloatArray): Matrix3f {
            require(array.size == 9) { "Expected 9 elements, but was ${array.size}" }
            return Matrix3f(
                Array(3) { col ->
                    Vector3f(array[col], array[3 + col], array[6 + col])
                },
            )
        }

        /**
         * @param array a 9-element [FloatArray] in column-major order, GLSL's own `mat3` order
         * @return the constructed [Matrix3f]
         */
        fun of(array: FloatArray): Matrix3f = ofColumnMajor(array)
    }
}
