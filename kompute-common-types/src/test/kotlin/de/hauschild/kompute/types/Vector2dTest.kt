package de.hauschild.kompute.types

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class Vector2dTest {
    @Test
    fun `toDoubleArray returns x and y in order`() {
        val vector = Vector2d(1.0, 2.0)

        assertThat(vector.toDoubleArray()).containsExactly(1.0, 2.0)
    }

    @Test
    fun `of constructs vector from array`() {
        val vector = Vector2d.of(doubleArrayOf(1.0, 2.0))

        assertThat(vector).isEqualTo(Vector2d(1.0, 2.0))
    }

    @Test
    fun `of rejects array with wrong size`() {
        assertThatThrownBy { Vector2d.of(doubleArrayOf(1.0)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 2 elements, but was 1")
    }

    @Test
    fun `roundtrips through byte array`() {
        val original = Vector2d(1.0, 2.0)

        val restored = original.toByteArray().fromByteArray(Vector2d::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `matches its std140 size`() {
        assertThat(Vector2d::class.sizeOf()).isEqualTo(16)
    }
}
