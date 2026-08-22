package de.hauschild.kompute.types

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class Vector3fTest {
    @Test
    fun `toFloatArray returns x, y and z in order`() {
        val vector = Vector3f(1f, 2f, 3f)

        assertThat(vector.toFloatArray()).containsExactly(1f, 2f, 3f)
    }

    @Test
    fun `of constructs vector from array`() {
        val vector = Vector3f.of(floatArrayOf(1f, 2f, 3f))

        assertThat(vector).isEqualTo(Vector3f(1f, 2f, 3f))
    }

    @Test
    fun `of rejects array with wrong size`() {
        assertThatThrownBy { Vector3f.of(floatArrayOf(1f, 2f)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 3 elements, but was 2")
    }

    @Test
    fun `roundtrips through byte array`() {
        val original = Vector3f(1f, 2f, 3f)

        val restored = original.toByteArray().fromByteArray(Vector3f::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `matches its std140 size`() {
        assertThat(Vector3f::class.sizeOf()).isEqualTo(12)
    }
}
