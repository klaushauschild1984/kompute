package de.hauschild.kompute.serialization.fixture

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.FixedSize
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct
import de.hauschild.kompute.types.Vector3f

/**
 * @property history the particle's last 8 positions, fixed size so it need not be the last field
 * @property velocity the particle's current velocity
 */
@GpuStruct
@Align(16)
data class Particle(
    @GpuField @FixedSize(8) val history: Array<Vector3f>,
    @GpuField val velocity: Vector3f,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is Particle && history.contentEquals(other.history) && velocity == other.velocity)

    override fun hashCode(): Int = 31 * history.contentHashCode() + velocity.hashCode()
}
