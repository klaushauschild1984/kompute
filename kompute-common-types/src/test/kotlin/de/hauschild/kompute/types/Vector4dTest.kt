package de.hauschild.kompute.types

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class Vector4dTest {
    @Test
    fun `toDoubleArray returns x, y, z and w in order`() {
        val vector = Vector4d(1.0, 2.0, 3.0, 4.0)

        assertThat(vector.toDoubleArray()).containsExactly(1.0, 2.0, 3.0, 4.0)
    }

    @Test
    fun `of constructs vector from array`() {
        val vector = Vector4d.of(doubleArrayOf(1.0, 2.0, 3.0, 4.0))

        assertThat(vector).isEqualTo(Vector4d(1.0, 2.0, 3.0, 4.0))
    }

    @Test
    fun `of rejects array with wrong size`() {
        assertThatThrownBy { Vector4d.of(doubleArrayOf(1.0, 2.0, 3.0)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 4 elements, but was 3")
    }

    @Test
    fun `roundtrips through byte array`() {
        val original = Vector4d(1.0, 2.0, 3.0, 4.0)

        val restored = original.toByteArray().fromByteArray(Vector4d::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `matches its std140 size`() {
        assertThat(Vector4d::class.sizeOf()).isEqualTo(32)
    }
}
