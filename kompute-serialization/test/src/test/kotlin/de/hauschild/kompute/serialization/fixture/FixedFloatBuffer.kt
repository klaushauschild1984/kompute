package de.hauschild.kompute.serialization.fixture

import de.hauschild.kompute.serialization.annotation.FixedSize
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * @property buffer a fixed size primitive array, followed by another field
 * @property scale a field after [buffer], only possible because [buffer] has a declared [FixedSize]
 */
@GpuStruct
data class FixedFloatBuffer(
    @GpuField @FixedSize(3) val buffer: FloatArray,
    @GpuField val scale: Float,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FixedFloatBuffer && buffer.contentEquals(other.buffer) && scale == other.scale)

    override fun hashCode(): Int = 31 * buffer.contentHashCode() + scale.hashCode()
}
