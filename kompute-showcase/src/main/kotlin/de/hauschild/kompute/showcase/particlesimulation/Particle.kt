package de.hauschild.kompute.showcase.particlesimulation

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct

/**
 * @property position
 * @property velocity
 * @property color
 * @property age
 */
@GpuStruct
class Particle(
    @GpuField val position: Vec2,
    @GpuField val velocity: Vec2,
    @GpuField val color: Vec3,
    @GpuField val age: Float,
)

/**
 * @property x
 * @property y
 */
@GpuStruct
@Align(8)
class Vec2(
    @GpuField val x: Float,
    @GpuField val y: Float,
)

/**
 * @property x
 * @property y
 * @property z
 */
@GpuStruct
@Align(16)
class Vec3(
    @GpuField val x: Float,
    @GpuField val y: Float,
    @GpuField val z: Float,
)
