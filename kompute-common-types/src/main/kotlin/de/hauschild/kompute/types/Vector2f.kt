package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * Matches GLSL's `vec2`.
 *
 * ```glsl
 * layout(std140, binding = 0) uniform Params {
 *     vec2 value;
 * };
 * ```
 *
 * @property x x coordinate
 * @property y y coordinate
 */
@GpuStruct
@Align(8)
data class Vector2f(
    @GpuField val x: Float,
    @GpuField val y: Float,
) {
    /**
     * @return the vector as a 2-element [FloatArray] in `[x,y]` order
     */
    fun toFloatArray(): FloatArray = floatArrayOf(x, y)

    companion object {
        /**
         * @param array a 2-element [FloatArray] in `[x,y]` order
         * @return the constructed [Vector2f]
         */
        fun of(array: FloatArray): Vector2f {
            require(array.size == 2) { "Expected 2 elements, but was ${array.size}" }
            return Vector2f(array[0], array[1])
        }
    }
}
