package de.hauschild.kompute.serialization.fixture

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * `vec3` in GLSL.
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
    @GpuField val z: Float
)
