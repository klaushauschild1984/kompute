package de.hauschild.kompute.types

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class Matrix4dTest {
    private val columnMajor =
        doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0)

    @Test
    fun `ofColumnMajor slices the array into columns directly`() {
        val matrix = Matrix4d.ofColumnMajor(columnMajor)

        assertThat(matrix.columns).containsExactly(
            Vector4d(1.0, 2.0, 3.0, 4.0),
            Vector4d(5.0, 6.0, 7.0, 8.0),
            Vector4d(9.0, 10.0, 11.0, 12.0),
            Vector4d(13.0, 14.0, 15.0, 16.0),
        )
    }

    @Test
    fun `ofRowMajor transposes rows into columns`() {
        val matrix = Matrix4d.ofRowMajor(columnMajor)

        assertThat(matrix.columns).containsExactly(
            Vector4d(1.0, 5.0, 9.0, 13.0),
            Vector4d(2.0, 6.0, 10.0, 14.0),
            Vector4d(3.0, 7.0, 11.0, 15.0),
            Vector4d(4.0, 8.0, 12.0, 16.0),
        )
    }

    @Test
    fun `of is an alias for ofColumnMajor`() {
        assertThat(Matrix4d.of(columnMajor)).isEqualTo(Matrix4d.ofColumnMajor(columnMajor))
    }

    @Test
    fun `toDoubleArray returns column-major order`() {
        assertThat(Matrix4d.ofColumnMajor(columnMajor).toDoubleArray()).containsExactly(*columnMajor)
    }

    @Test
    fun `equals and hashCode compare column contents`() {
        val a = Matrix4d.ofColumnMajor(columnMajor)
        val b = Matrix4d.ofColumnMajor(columnMajor.copyOf())

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
    }

    @Test
    fun `ofColumnMajor rejects array with wrong size`() {
        assertThatThrownBy { Matrix4d.ofColumnMajor(doubleArrayOf(1.0, 2.0)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 16 elements, but was 2")
    }

    @Test
    fun `ofRowMajor rejects array with wrong size`() {
        assertThatThrownBy { Matrix4d.ofRowMajor(doubleArrayOf(1.0, 2.0)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 16 elements, but was 2")
    }

    @Test
    fun `roundtrips through byte array`() {
        val original = Matrix4d.ofColumnMajor(columnMajor)

        val restored = original.toByteArray().fromByteArray(Matrix4d::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `matches its std140 size`() {
        assertThat(Matrix4d::class.sizeOf()).isEqualTo(128)
    }
}
