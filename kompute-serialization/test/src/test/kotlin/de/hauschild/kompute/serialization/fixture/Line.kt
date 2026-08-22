package de.hauschild.kompute.serialization.fixture

import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct
import de.hauschild.kompute.types.Vector3f

/**
 * @property start
 * @property end
 */
@GpuStruct
data class Line(
    @GpuField val start: Vector3f,
    @GpuField val end: Vector3f
)
