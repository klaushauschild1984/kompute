package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * Matches GLSL's `dvec2`.
 *
 * ```glsl
 * layout(std140, binding = 0) uniform Params {
 *     dvec2 value;
 * };
 * ```
 *
 * `dvec2` requires GLSL 4.00+ or the `GL_ARB_gpu_shader_fp64` extension — not available in GLSL ES/WebGL.
 *
 * @property x x coordinate
 * @property y y coordinate
 */
@GpuStruct
@Align(16)
data class Vector2d(
    @GpuField val x: Double,
    @GpuField val y: Double,
) {
    /**
     * @return the vector as a 2-element [DoubleArray] in `[x,y]` order
     */
    fun toDoubleArray(): DoubleArray = doubleArrayOf(x, y)

    companion object {
        /**
         * @param array a 2-element [DoubleArray] in `[x,y]` order
         * @return the constructed [Vector2d]
         */
        fun of(array: DoubleArray): Vector2d {
            require(array.size == 2) { "Expected 2 elements, but was ${array.size}" }
            return Vector2d(array[0], array[1])
        }
    }
}
