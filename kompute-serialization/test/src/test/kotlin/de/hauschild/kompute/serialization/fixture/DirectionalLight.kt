package de.hauschild.kompute.serialization.fixture

import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 *
 * @property direction
 * @property intensity
 * @property color
 * @property ambient
 */
@GpuStruct
data class DirectionalLight(
    @GpuField val direction: Vector3f,
    @GpuField val intensity: Float,
    @GpuField val color: Vector3f,
    @GpuField val ambient: Float,
)
