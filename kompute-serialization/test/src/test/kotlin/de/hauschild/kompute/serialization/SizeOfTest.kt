package de.hauschild.kompute.serialization

import de.hauschild.kompute.serialization.annotation.Layout
import de.hauschild.kompute.serialization.fixture.DirectionalLight
import de.hauschild.kompute.serialization.fixture.FixedFloatBuffer
import de.hauschild.kompute.serialization.fixture.FloatBuffer
import de.hauschild.kompute.serialization.fixture.Line
import de.hauschild.kompute.serialization.fixture.Particle
import de.hauschild.kompute.serialization.fixture.ParticleSystem
import de.hauschild.kompute.serialization.fixture.SingleFloat
import de.hauschild.kompute.serialization.fixture.Vector3f
import de.hauschild.kompute.serialization.fixture.Vector3fArray
import de.hauschild.kompute.serialization.fixture.sizeOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SizeOfTest {
    @Test
    fun `single float`() {
        assertThat(SingleFloat::class.sizeOf()).isEqualTo(4)
    }

    @Test
    fun `vec3`() {
        assertThat(Vector3f::class.sizeOf()).isEqualTo(12)
    }

    @Test
    fun `light with vec3 position`() {
        assertThat(DirectionalLight::class.sizeOf()).isEqualTo(32)
    }

    @Test
    fun `line with start and end as vec3`() {
        assertThat(Line::class.sizeOf()).isEqualTo(32)
    }

    @Test
    fun `float array std140`() {
        assertThat(FloatBuffer::class.sizeOf(3)).isEqualTo(48)
    }

    @Test
    fun `float array std430`() {
        assertThat(FloatBuffer::class.sizeOf(3, Layout.STD430)).isEqualTo(12)
    }

    @Test
    fun `vec3 array std140`() {
        assertThat(Vector3fArray::class.sizeOf(2)).isEqualTo(32)
    }

    @Test
    fun `fixed size struct array not last field`() {
        assertThat(Particle::class.sizeOf()).isEqualTo(144)
    }

    @Test
    fun `fixed size primitive array std140`() {
        assertThat(FixedFloatBuffer::class.sizeOf()).isEqualTo(64)
    }

    @Test
    fun `fixed size primitive array std430`() {
        assertThat(FixedFloatBuffer::class.sizeOf(Layout.STD430)).isEqualTo(16)
    }

    @Test
    fun `struct with fixed size array field nested inside another struct`() {
        assertThat(ParticleSystem::class.sizeOf()).isEqualTo(160)
    }
}
