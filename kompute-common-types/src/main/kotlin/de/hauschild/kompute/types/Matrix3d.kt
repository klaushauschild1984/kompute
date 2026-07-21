package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.FixedSize
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * Matches GLSL's `dmat3`, stored column-major exactly as GLSL does.
 *
 * ```glsl
 * layout(std140, binding = 0) uniform Params {
 *     dmat3 value;
 * };
 * ```
 *
 * `dmat3` requires GLSL 4.00+ or the `GL_ARB_gpu_shader_fp64` extension — not available in GLSL ES/WebGL.
 *
 * Use [ofColumnMajor] (or [of], its alias) when the source data is already column-major, e.g.
 * copied straight out of a shader or another GLSL-facing library. Use [ofRowMajor] when building
 * from row-major data, the layout most math textbooks and some other engines use.
 *
 * @property columns the 3 column vectors, GLSL's own `mat3` storage order
 */
@GpuStruct
@Align(32)
data class Matrix3d(
    @GpuField @FixedSize(3) val columns: Array<Vector3d>,
) {
    /**
     * @return the matrix as a 9-element [DoubleArray] in column-major order
     */
    fun toDoubleArray(): DoubleArray = DoubleArray(9) { i -> columns[i / 3].toDoubleArray()[i % 3] }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Matrix3d && columns.contentEquals(other.columns))

    override fun hashCode(): Int = columns.contentHashCode()

    companion object {
        /**
         * @param array a 9-element [DoubleArray] in column-major order
         * @return the constructed [Matrix3d]
         */
        fun ofColumnMajor(array: DoubleArray): Matrix3d {
            require(array.size == 9) { "Expected 9 elements, but was ${array.size}" }
            return Matrix3d(
                Array(3) { col ->
                    Vector3d(array[col * 3], array[col * 3 + 1], array[col * 3 + 2])
                },
            )
        }

        /**
         * @param array a 9-element [DoubleArray] in row-major order
         * @return the constructed [Matrix3d]
         */
        fun ofRowMajor(array: DoubleArray): Matrix3d {
            require(array.size == 9) { "Expected 9 elements, but was ${array.size}" }
            return Matrix3d(
                Array(3) { col ->
                    Vector3d(array[col], array[3 + col], array[6 + col])
                },
            )
        }

        /**
         * @param array a 9-element [DoubleArray] in column-major order, GLSL's own `mat3` order
         * @return the constructed [Matrix3d]
         */
        fun of(array: DoubleArray): Matrix3d = ofColumnMajor(array)
    }
}
