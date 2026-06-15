package de.hauschild.kompute.serialization.fixture

import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * @property buffer
 */
@GpuStruct
class Vector3fArray(@GpuField val buffer: Array<Vector3f>)
