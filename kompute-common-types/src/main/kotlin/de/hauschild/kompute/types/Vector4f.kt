package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * @property x x coordinate
 * @property y y coordinate
 * @property z z coordinate
 * @property w w coordinate
 */
@GpuStruct
@Align(16)
data class Vector4f(
    @GpuField val x: Float,
    @GpuField val y: Float,
    @GpuField val z: Float,
    @GpuField val w: Float,
) {
    /**
     * @return the vector as a 4-element [FloatArray] in `[x,y,z,w]` order
     */
    fun toFloatArray(): FloatArray = floatArrayOf(x, y, z, w)

    companion object {
        /**
         * @param array a 4-element [FloatArray] in `[x,y,z,w]` order
         * @return the constructed [Vector4f]
         */
        fun of(array: FloatArray): Vector4f {
            require(array.size == 4) { "Expected 4 elements, but was ${array.size}" }
            return Vector4f(array[0], array[1], array[2], array[3])
        }
    }
}
