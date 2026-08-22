package de.hauschild.kompute.types

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class Vector2fTest {
    @Test
    fun `toFloatArray returns x and y in order`() {
        val vector = Vector2f(1f, 2f)

        assertThat(vector.toFloatArray()).containsExactly(1f, 2f)
    }

    @Test
    fun `of constructs vector from array`() {
        val vector = Vector2f.of(floatArrayOf(1f, 2f))

        assertThat(vector).isEqualTo(Vector2f(1f, 2f))
    }

    @Test
    fun `of rejects array with wrong size`() {
        assertThatThrownBy { Vector2f.of(floatArrayOf(1f)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 2 elements, but was 1")
    }

    @Test
    fun `roundtrips through byte array`() {
        val original = Vector2f(1f, 2f)

        val restored = original.toByteArray().fromByteArray(Vector2f::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `matches its std140 size`() {
        assertThat(Vector2f::class.sizeOf()).isEqualTo(8)
    }
}
