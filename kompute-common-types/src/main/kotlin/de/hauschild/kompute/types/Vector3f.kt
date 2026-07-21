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
@Align(16)
data class Vector3f(
    @GpuField val x: Float,
    @GpuField val y: Float,
    @GpuField val z: Float,
)
