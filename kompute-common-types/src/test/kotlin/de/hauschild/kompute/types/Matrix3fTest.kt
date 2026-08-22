package de.hauschild.kompute.types

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class Matrix3fTest {
    private val columnMajor = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f)

    @Test
    fun `ofColumnMajor slices the array into columns directly`() {
        val matrix = Matrix3f.ofColumnMajor(columnMajor)

        assertThat(matrix.columns).containsExactly(
            Vector3f(1f, 2f, 3f),
            Vector3f(4f, 5f, 6f),
            Vector3f(7f, 8f, 9f),
        )
    }

    @Test
    fun `ofRowMajor transposes rows into columns`() {
        val matrix = Matrix3f.ofRowMajor(columnMajor)

        assertThat(matrix.columns).containsExactly(
            Vector3f(1f, 4f, 7f),
            Vector3f(2f, 5f, 8f),
            Vector3f(3f, 6f, 9f),
        )
    }

    @Test
    fun `of is an alias for ofColumnMajor`() {
        assertThat(Matrix3f.of(columnMajor)).isEqualTo(Matrix3f.ofColumnMajor(columnMajor))
    }

    @Test
    fun `toFloatArray returns column-major order`() {
        assertThat(Matrix3f.ofColumnMajor(columnMajor).toFloatArray()).containsExactly(*columnMajor)
    }

    @Test
    fun `equals and hashCode compare column contents`() {
        val a = Matrix3f.ofColumnMajor(columnMajor)
        val b = Matrix3f.ofColumnMajor(columnMajor.copyOf())

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
    }

    @Test
    fun `ofColumnMajor rejects array with wrong size`() {
        assertThatThrownBy { Matrix3f.ofColumnMajor(floatArrayOf(1f, 2f)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 9 elements, but was 2")
    }

    @Test
    fun `ofRowMajor rejects array with wrong size`() {
        assertThatThrownBy { Matrix3f.ofRowMajor(floatArrayOf(1f, 2f)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 9 elements, but was 2")
    }

    @Test
    fun `roundtrips through byte array respecting std140 column padding`() {
        val original = Matrix3f.ofColumnMajor(columnMajor)

        val restored = original.toByteArray().fromByteArray(Matrix3f::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `matches its std140 size`() {
        assertThat(Matrix3f::class.sizeOf()).isEqualTo(48)
    }
}
