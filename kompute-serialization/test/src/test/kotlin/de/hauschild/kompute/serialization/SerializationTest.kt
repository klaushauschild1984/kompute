package de.hauschild.kompute.serialization

import de.hauschild.kompute.serialization.annotation.Layout
import de.hauschild.kompute.serialization.fixture.DirectionalLight
import de.hauschild.kompute.serialization.fixture.FloatBuffer
import de.hauschild.kompute.serialization.fixture.Line
import de.hauschild.kompute.serialization.fixture.SingleFloat
import de.hauschild.kompute.serialization.fixture.Vector3f
import de.hauschild.kompute.serialization.fixture.Vector3fArray
import de.hauschild.kompute.serialization.fixture.toByteArray
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationTest {
    @Test
    fun `single float std140`() {
        val bytes = SingleFloat(value = 3.14f).toByteArray()

        assertEquals(4, bytes.size)
        assertEquals(3.14f, ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float)
    }

    @Test
    fun `vec3 std140`() {
        val bytes = Vector3f(3f, 4f, 5f).toByteArray()

        assertEquals(12, bytes.size)
        val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(3f, byteBuffer.float)
        assertEquals(4f, byteBuffer.float)
        assertEquals(5f, byteBuffer.float)
    }

    @Test
    fun `light with vec3 position std140`() {
        val bytes = DirectionalLight(
            direction = Vector3f(1f, 2f, 3f),
            intensity = 0.5f,
            color = Vector3f(4f, 5f, 6f),
            ambient = 1f,
        ).toByteArray()

        assertEquals(32, bytes.size)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1f, buffer.float)
        assertEquals(2f, buffer.float)
        assertEquals(3f, buffer.float)
        assertEquals(0.5f, buffer.float)
        assertEquals(4f, buffer.float)
        assertEquals(5f, buffer.float)
        assertEquals(6f, buffer.float)
        assertEquals(1f, buffer.float)
    }

    @Test
    fun `light with start and end as vec3`() {
        val bytes = Line(
            start = Vector3f(1f, 2f, 3f),
            end = Vector3f(4f, 5f, 6f),
        ).toByteArray()

        assertEquals(32, bytes.size)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1f, buffer.float)
        assertEquals(2f, buffer.float)
        assertEquals(3f, buffer.float)
        assertEquals(0f, buffer.float)
        assertEquals(4f, buffer.float)
        assertEquals(5f, buffer.float)
        assertEquals(6f, buffer.float)
        assertEquals(0f, buffer.float)
    }

    @Test
    fun `float array std140`() {
        val bytes = FloatBuffer(floatArrayOf(1f, 2f, 3f)).toByteArray()

        assertEquals(48, bytes.size)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1f, buffer.float)
        buffer.position(16)
        assertEquals(2f, buffer.float)
        buffer.position(32)
        assertEquals(3f, buffer.float)
    }

    @Test
    fun `float array std430`() {
        val bytes = FloatBuffer(floatArrayOf(1f, 2f, 3f)).toByteArray(Layout.STD430)

        assertEquals(12, bytes.size)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1f, buffer.float)
        assertEquals(2f, buffer.float)
        assertEquals(3f, buffer.float)
    }

    @Test
    fun `vec3 array std140`() {
        val bytes = Vector3fArray(arrayOf(
            Vector3f(1f, 2f, 3f),
            Vector3f(4f, 5f, 6f)
        )).toByteArray()

        assertEquals(32, bytes.size)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1f, buffer.float)
        assertEquals(2f, buffer.float)
        assertEquals(3f, buffer.float)
        buffer.position(16)
        assertEquals(4f, buffer.float)
        assertEquals(5f, buffer.float)
        assertEquals(6f, buffer.float)
    }
}
