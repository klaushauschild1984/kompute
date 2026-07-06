package de.hauschild.kompute.serialization

import de.hauschild.kompute.serialization.annotation.Layout
import de.hauschild.kompute.serialization.fixture.DirectionalLight
import de.hauschild.kompute.serialization.fixture.FloatBuffer
import de.hauschild.kompute.serialization.fixture.Line
import de.hauschild.kompute.serialization.fixture.SingleFloat
import de.hauschild.kompute.serialization.fixture.Vector3f
import de.hauschild.kompute.serialization.fixture.Vector3fArray
import de.hauschild.kompute.serialization.fixture.toByteArray
import org.assertj.core.api.Assertions.assertThat
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
            intensity = 0.5f,
            color = Vector3f(4f, 5f, 6f),
            ambient = 1f,
        ).toByteArray()

        assertThat(bytes).hasSize(32)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertThat(buffer.float).isEqualTo(1f)
        assertThat(buffer.float).isEqualTo(2f)
        assertThat(buffer.float).isEqualTo(3f)
        assertThat(buffer.float).isEqualTo(0.5f)
        assertThat(buffer.float).isEqualTo(4f)
        assertThat(buffer.float).isEqualTo(5f)
        assertThat(buffer.float).isEqualTo(6f)
        assertThat(buffer.float).isEqualTo(1f)
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
}
