package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * Matches GLSL's `dvec4`.
 *
 * ```glsl
 * layout(std140, binding = 0) uniform Params {
 *     dvec4 value;
 * };
 * ```
 *
 * `dvec4` requires GLSL 4.00+ or the `GL_ARB_gpu_shader_fp64` extension — not available in GLSL ES/WebGL.
 *
 * @property x x coordinate
 * @property y y coordinate
 * @property z z coordinate
 * @property w w coordinate
 */
@GpuStruct
@Align(32)
data class Vector4d(
    @GpuField val x: Double,
    @GpuField val y: Double,
    @GpuField val z: Double,
    @GpuField val w: Double,
) {
    /**
     * @return the vector as a 4-element [DoubleArray] in `[x,y,z,w]` order
     */
    fun toDoubleArray(): DoubleArray = doubleArrayOf(x, y, z, w)

    companion object {
        /**
         * @param array a 4-element [DoubleArray] in `[x,y,z,w]` order
         * @return the constructed [Vector4d]
         */
        fun of(array: DoubleArray): Vector4d {
            require(array.size == 4) { "Expected 4 elements, but was ${array.size}" }
            return Vector4d(array[0], array[1], array[2], array[3])
        }
    }
}
