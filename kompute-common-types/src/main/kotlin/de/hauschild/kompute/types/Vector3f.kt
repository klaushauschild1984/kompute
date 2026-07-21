package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * Matches GLSL's `vec3`.
 *
 * ```glsl
 * layout(std140, binding = 0) uniform Params {
 *     vec3 value;
 * };
 * ```
 *
 * @property x x coordinate
 * @property y y coordinate
 * @property z z coordinate
 */
@GpuStruct
@Align(16)
data class Vector3f(
    @GpuField val x: Float,
    @GpuField val y: Float,
    @GpuField val z: Float,
) {
    /**
     * @return the vector as 3-element [FloatArray] in `[x,y,z]` order
     */
    fun toFloatArray(): FloatArray = floatArrayOf(x, y, z)

    companion object {
        /**
         * @param array a 3-element [FloatArray] in `[x,y,z]` order
         * @return the constructed [Vector3f]
         */
        fun of(array: FloatArray): Vector3f {
            require(array.size == 3) { "Expected 3 elements, but was ${array.size}" }
            return Vector3f(array[0], array[1], array[2])
        }
    }
}
