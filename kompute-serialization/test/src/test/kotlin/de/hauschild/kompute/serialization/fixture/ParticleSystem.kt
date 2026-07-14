package de.hauschild.kompute.serialization.fixture

import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * Nests [Particle], which has a [de.hauschild.kompute.serialization.annotation.FixedSize] array
 * field — this would previously be rejected as a "dynamic array field" nesting.
 *
 * @property particle the nested particle
 * @property count number of active particles
 */
@GpuStruct
data class ParticleSystem(
    @GpuField val particle: Particle,
    @GpuField val count: Int,
)
