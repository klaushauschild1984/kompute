package de.hauschild.kompute.types

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class Vector4fTest {
    @Test
    fun `toFloatArray returns x, y, z and w in order`() {
        val vector = Vector4f(1f, 2f, 3f, 4f)

        assertThat(vector.toFloatArray()).containsExactly(1f, 2f, 3f, 4f)
    }

    @Test
    fun `of constructs vector from array`() {
        val vector = Vector4f.of(floatArrayOf(1f, 2f, 3f, 4f))

        assertThat(vector).isEqualTo(Vector4f(1f, 2f, 3f, 4f))
    }

    @Test
    fun `of rejects array with wrong size`() {
        assertThatThrownBy { Vector4f.of(floatArrayOf(1f, 2f, 3f)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 4 elements, but was 3")
    }

    @Test
    fun `roundtrips through byte array`() {
        val original = Vector4f(1f, 2f, 3f, 4f)

        val restored = original.toByteArray().fromByteArray(Vector4f::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `matches its std140 size`() {
        assertThat(Vector4f::class.sizeOf()).isEqualTo(16)
    }
}
