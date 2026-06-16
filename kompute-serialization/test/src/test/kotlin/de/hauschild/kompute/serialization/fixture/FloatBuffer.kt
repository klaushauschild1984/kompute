package de.hauschild.kompute.serialization.fixture

import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * @property buffer
 */
@GpuStruct
class FloatBuffer(@GpuField val buffer: FloatArray)
