package de.hauschild.kompute.types

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class Vector3dTest {
    @Test
    fun `toDoubleArray returns x, y and z in order`() {
        val vector = Vector3d(1.0, 2.0, 3.0)

        assertThat(vector.toDoubleArray()).containsExactly(1.0, 2.0, 3.0)
    }

    @Test
    fun `of constructs vector from array`() {
        val vector = Vector3d.of(doubleArrayOf(1.0, 2.0, 3.0))

        assertThat(vector).isEqualTo(Vector3d(1.0, 2.0, 3.0))
    }

    @Test
    fun `of rejects array with wrong size`() {
        assertThatThrownBy { Vector3d.of(doubleArrayOf(1.0, 2.0)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 3 elements, but was 2")
    }

    @Test
    fun `roundtrips through byte array`() {
        val original = Vector3d(1.0, 2.0, 3.0)

        val restored = original.toByteArray().fromByteArray(Vector3d::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `matches its std140 size`() {
        assertThat(Vector3d::class.sizeOf()).isEqualTo(24)
    }
}
