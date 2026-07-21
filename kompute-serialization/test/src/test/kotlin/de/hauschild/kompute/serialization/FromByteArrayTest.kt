package de.hauschild.kompute.serialization

import de.hauschild.kompute.serialization.annotation.Layout
import de.hauschild.kompute.serialization.fixture.DirectionalLight
import de.hauschild.kompute.serialization.fixture.FixedDoubleBuffer
import de.hauschild.kompute.serialization.fixture.FixedFloatBuffer
import de.hauschild.kompute.serialization.fixture.FloatBuffer
import de.hauschild.kompute.serialization.fixture.Line
import de.hauschild.kompute.serialization.fixture.Particle
import de.hauschild.kompute.serialization.fixture.ParticleSystem
import de.hauschild.kompute.serialization.fixture.SingleFloat
import de.hauschild.kompute.serialization.fixture.Vector3fArray
import de.hauschild.kompute.serialization.fixture.fromByteArray
import de.hauschild.kompute.serialization.fixture.toByteArray
import de.hauschild.kompute.types.Vector3f
import de.hauschild.kompute.types.fromByteArray
import de.hauschild.kompute.types.toByteArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FromByteArrayTest {
    @Test
    fun `single float roundtrip`() {
        val original = SingleFloat(value = 3.14f)

        val restored = original.toByteArray().fromByteArray(SingleFloat::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `vec3 roundtrip`() {
        val original = Vector3f(3f, 4f, 5f)

        val restored = original.toByteArray().fromByteArray(Vector3f::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `light with vec3 position roundtrip`() {
        val original = DirectionalLight(
            direction = Vector3f(1f, 2f, 3f),
            intensity = 0.5f,
            color = Vector3f(4f, 5f, 6f),
            ambient = 1f,
        )

        val restored = original.toByteArray().fromByteArray(DirectionalLight::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `line with start and end as vec3 roundtrip`() {
        val original = Line(
            start = Vector3f(1f, 2f, 3f),
            end = Vector3f(4f, 5f, 6f),
        )

        val restored = original.toByteArray().fromByteArray(Line::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `float array std140 roundtrip`() {
        val original = FloatBuffer(floatArrayOf(1f, 2f, 3f))

        val restored = original.toByteArray().fromByteArray(FloatBuffer::class)

        assertThat(restored.buffer).containsExactly(1f, 2f, 3f)
    }

    @Test
    fun `float array std430 roundtrip`() {
        val original = FloatBuffer(floatArrayOf(1f, 2f, 3f))

        val restored = original.toByteArray(Layout.STD430).fromByteArray(FloatBuffer::class, Layout.STD430)

        assertThat(restored.buffer).containsExactly(1f, 2f, 3f)
    }

    @Test
    fun `vec3 array std140 roundtrip`() {
        val original = Vector3fArray(arrayOf(Vector3f(1f, 2f, 3f), Vector3f(4f, 5f, 6f)))

        val restored = original.toByteArray().fromByteArray(Vector3fArray::class)

        assertThat(restored.buffer).containsExactly(Vector3f(1f, 2f, 3f), Vector3f(4f, 5f, 6f))
    }

    @Test
    fun `fixed size struct array not last field roundtrip`() {
        val original = Particle(
            history = Array(8) { i -> Vector3f(i * 10f, i * 10f + 1f, i * 10f + 2f) },
            velocity = Vector3f(100f, 200f, 300f),
        )

        val restored = original.toByteArray().fromByteArray(Particle::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `fixed size primitive array std140 roundtrip`() {
        val original = FixedFloatBuffer(floatArrayOf(1f, 2f, 3f), scale = 4f)

        val restored = original.toByteArray().fromByteArray(FixedFloatBuffer::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `fixed size primitive array std430 roundtrip`() {
        val original = FixedFloatBuffer(floatArrayOf(1f, 2f, 3f), scale = 4f)

        val restored = original.toByteArray(Layout.STD430).fromByteArray(FixedFloatBuffer::class, Layout.STD430)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `fixed size double array std140 roundtrip`() {
        val original = FixedDoubleBuffer(doubleArrayOf(1.0, 2.0, 3.0), scale = 4.0)

        val restored = original.toByteArray().fromByteArray(FixedDoubleBuffer::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `fixed size double array std430 roundtrip`() {
        val original = FixedDoubleBuffer(doubleArrayOf(1.0, 2.0, 3.0), scale = 4.0)

        val restored = original.toByteArray(Layout.STD430).fromByteArray(FixedDoubleBuffer::class, Layout.STD430)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `struct with fixed size array field nested inside another struct roundtrip`() {
        val original = ParticleSystem(
            particle = Particle(
                history = Array(8) { i -> Vector3f(i.toFloat(), i.toFloat(), i.toFloat()) },
                velocity = Vector3f(1f, 2f, 3f),
            ),
            count = 5,
        )

        val restored = original.toByteArray().fromByteArray(ParticleSystem::class)

        assertThat(restored).isEqualTo(original)
    }
}
