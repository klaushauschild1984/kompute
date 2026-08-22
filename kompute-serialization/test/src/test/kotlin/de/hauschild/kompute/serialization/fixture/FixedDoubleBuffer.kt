package de.hauschild.kompute.serialization.fixture

import de.hauschild.kompute.serialization.annotation.FixedSize
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * @property buffer a fixed size double array, followed by another field
 * @property scale a double field after [buffer], only possible because [buffer] has a declared [FixedSize]
 */
@GpuStruct
data class FixedDoubleBuffer(
    @GpuField @FixedSize(3) val buffer: DoubleArray,
    @GpuField val scale: Double,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FixedDoubleBuffer && buffer.contentEquals(other.buffer) && scale == other.scale)

    override fun hashCode(): Int = 31 * buffer.contentHashCode() + scale.hashCode()
}
