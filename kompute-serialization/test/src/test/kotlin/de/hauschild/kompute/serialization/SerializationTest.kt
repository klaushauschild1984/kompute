package de.hauschild.kompute.serialization

import de.hauschild.kompute.serialization.annotation.Layout
import de.hauschild.kompute.serialization.fixture.FixedDoubleBuffer
import de.hauschild.kompute.serialization.fixture.FixedFloatBuffer
import de.hauschild.kompute.serialization.fixture.FloatBuffer
import de.hauschild.kompute.serialization.fixture.Line
import de.hauschild.kompute.serialization.fixture.Particle
import de.hauschild.kompute.serialization.fixture.SingleFloat
import de.hauschild.kompute.serialization.fixture.Vector3fArray
import de.hauschild.kompute.serialization.fixture.toByteArray
import de.hauschild.kompute.types.DirectionalLight
import de.hauschild.kompute.types.Vector3f
import de.hauschild.kompute.types.toByteArray
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SerializationTest {
    @Test
    fun `single float std140`() {
        val bytes = SingleFloat(value = 3.14f).toByteArray()

        assertThat(bytes).hasSize(4)
        assertThat(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float).isEqualTo(3.14f)
    }

    @Test
    fun `vec3 std140`() {
        val bytes = Vector3f(3f, 4f, 5f).toByteArray()

        assertThat(bytes).hasSize(12)
        val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertThat(byteBuffer.float).isEqualTo(3f)
        assertThat(byteBuffer.float).isEqualTo(4f)
        assertThat(byteBuffer.float).isEqualTo(5f)
    }

    @Test
    fun `light with vec3 position std140`() {
        val bytes = DirectionalLight(
            direction = Vector3f(1f, 2f, 3f),
            color = Vector3f(4f, 5f, 6f),
            intensity = 0.7f,
        ).toByteArray()

        assertThat(bytes).hasSize(32)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertThat(buffer.float).isEqualTo(1f)
        assertThat(buffer.float).isEqualTo(2f)
        assertThat(buffer.float).isEqualTo(3f)
        buffer.position(16)
        assertThat(buffer.float).isEqualTo(4f)
        assertThat(buffer.float).isEqualTo(5f)
        assertThat(buffer.float).isEqualTo(6f)
        assertThat(buffer.float).isEqualTo(0.7f)
    }

    @Test
    fun `light with start and end as vec3`() {
        val bytes = Line(
            start = Vector3f(1f, 2f, 3f),
            end = Vector3f(4f, 5f, 6f),
        ).toByteArray()

        assertThat(bytes).hasSize(32)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertThat(buffer.float).isEqualTo(1f)
        assertThat(buffer.float).isEqualTo(2f)
        assertThat(buffer.float).isEqualTo(3f)
        assertThat(buffer.float).isEqualTo(0f)
        assertThat(buffer.float).isEqualTo(4f)
        assertThat(buffer.float).isEqualTo(5f)
        assertThat(buffer.float).isEqualTo(6f)
        assertThat(buffer.float).isEqualTo(0f)
    }

    @Test
    fun `float array std140`() {
        val bytes = FloatBuffer(floatArrayOf(1f, 2f, 3f)).toByteArray()

        assertThat(bytes).hasSize(48)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertThat(buffer.float).isEqualTo(1f)
        buffer.position(16)
        assertThat(buffer.float).isEqualTo(2f)
        buffer.position(32)
        assertThat(buffer.float).isEqualTo(3f)
    }

    @Test
    fun `float array std430`() {
        val bytes = FloatBuffer(floatArrayOf(1f, 2f, 3f)).toByteArray(Layout.STD430)

        assertThat(bytes).hasSize(12)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertThat(buffer.float).isEqualTo(1f)
        assertThat(buffer.float).isEqualTo(2f)
        assertThat(buffer.float).isEqualTo(3f)
    }

    @Test
    fun `vec3 array std140`() {
        val bytes = Vector3fArray(arrayOf(
            Vector3f(1f, 2f, 3f),
            Vector3f(4f, 5f, 6f)
        )).toByteArray()

        assertThat(bytes).hasSize(32)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertThat(buffer.float).isEqualTo(1f)
        assertThat(buffer.float).isEqualTo(2f)
        assertThat(buffer.float).isEqualTo(3f)
        buffer.position(16)
        assertThat(buffer.float).isEqualTo(4f)
        assertThat(buffer.float).isEqualTo(5f)
        assertThat(buffer.float).isEqualTo(6f)
    }

    @Test
    fun `fixed size struct array not last field`() {
        val history = Array(8) { i -> Vector3f(i * 10f, i * 10f + 1f, i * 10f + 2f) }
        val bytes = Particle(history, velocity = Vector3f(100f, 200f, 300f)).toByteArray()

        assertThat(bytes).hasSize(144)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertThat(buffer.float).isEqualTo(0f)
        buffer.position(16 * 7)
        assertThat(buffer.float).isEqualTo(70f)
        buffer.position(128)
        assertThat(buffer.float).isEqualTo(100f)
        assertThat(buffer.float).isEqualTo(200f)
        assertThat(buffer.float).isEqualTo(300f)
    }

    @Test
    fun `fixed size array with wrong length throws`() {
        val history = Array(7) { Vector3f(0f, 0f, 0f) }
        val particle = Particle(history, velocity = Vector3f(0f, 0f, 0f))

        assertThatThrownBy { particle.toByteArray() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 8 elements for 'history', but was 7")
    }

    @Test
    fun `fixed size primitive array std140`() {
        val bytes = FixedFloatBuffer(floatArrayOf(1f, 2f, 3f), scale = 4f).toByteArray()

        assertThat(bytes).hasSize(64)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertThat(buffer.float).isEqualTo(1f)
        buffer.position(16)
        assertThat(buffer.float).isEqualTo(2f)
        buffer.position(32)
        assertThat(buffer.float).isEqualTo(3f)
        buffer.position(48)
        assertThat(buffer.float).isEqualTo(4f)
    }

    @Test
    fun `fixed size primitive array std430`() {
        val bytes = FixedFloatBuffer(floatArrayOf(1f, 2f, 3f), scale = 4f).toByteArray(Layout.STD430)

        assertThat(bytes).hasSize(16)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertThat(buffer.float).isEqualTo(1f)
        assertThat(buffer.float).isEqualTo(2f)
        assertThat(buffer.float).isEqualTo(3f)
        assertThat(buffer.float).isEqualTo(4f)
    }

    @Test
    fun `fixed size double array std140`() {
        val bytes = FixedDoubleBuffer(doubleArrayOf(1.0, 2.0, 3.0), scale = 4.0).toByteArray()

        assertThat(bytes).hasSize(64)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertThat(buffer.double).isEqualTo(1.0)
        buffer.position(16)
        assertThat(buffer.double).isEqualTo(2.0)
        buffer.position(32)
        assertThat(buffer.double).isEqualTo(3.0)
        buffer.position(48)
        assertThat(buffer.double).isEqualTo(4.0)
    }

    @Test
    fun `fixed size double array std430`() {
        val bytes = FixedDoubleBuffer(doubleArrayOf(1.0, 2.0, 3.0), scale = 4.0).toByteArray(Layout.STD430)

        assertThat(bytes).hasSize(32)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertThat(buffer.double).isEqualTo(1.0)
        assertThat(buffer.double).isEqualTo(2.0)
        assertThat(buffer.double).isEqualTo(3.0)
        assertThat(buffer.double).isEqualTo(4.0)
    }
}
