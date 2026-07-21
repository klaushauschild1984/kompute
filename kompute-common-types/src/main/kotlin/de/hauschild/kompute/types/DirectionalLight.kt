package de.hauschild.kompute.types

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * @property direction the direction the light is shining, e.g. a normalized vector
 * @property color the light's color
 * @property intensity the light's intensity
 */
@GpuStruct
@Align(16)
data class DirectionalLight(
    @GpuField val direction: Vector3f,
    @GpuField val color: Vector3f,
    @GpuField val intensity: Float,
)
