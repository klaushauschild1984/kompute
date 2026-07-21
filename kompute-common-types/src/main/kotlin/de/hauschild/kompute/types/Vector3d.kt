package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * @property x x coordinate
 * @property y y coordinate
 * @property z z coordinate
 */
@GpuStruct
@Align(32)
data class Vector3d(
    @GpuField val x: Double,
    @GpuField val y: Double,
    @GpuField val z: Double,
) {
    /**
     * @return the vector as a 3-element [DoubleArray] in `[x,y,z]` order
     */
    fun toDoubleArray(): DoubleArray = doubleArrayOf(x, y, z)

    companion object {
        /**
         * @param array a 3-element [DoubleArray] in `[x,y,z]` order
         * @return the constructed [Vector3d]
         */
        fun of(array: DoubleArray): Vector3d {
            require(array.size == 3) { "Expected 3 elements, but was ${array.size}" }
            return Vector3d(array[0], array[1], array[2])
        }
    }
}
