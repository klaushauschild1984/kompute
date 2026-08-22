package de.hauschild.kompute.types

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class Matrix3dTest {
    private val columnMajor = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0)

    @Test
    fun `ofColumnMajor slices the array into columns directly`() {
        val matrix = Matrix3d.ofColumnMajor(columnMajor)

        assertThat(matrix.columns).containsExactly(
            Vector3d(1.0, 2.0, 3.0),
            Vector3d(4.0, 5.0, 6.0),
            Vector3d(7.0, 8.0, 9.0),
        )
    }

    @Test
    fun `ofRowMajor transposes rows into columns`() {
        val matrix = Matrix3d.ofRowMajor(columnMajor)

        assertThat(matrix.columns).containsExactly(
            Vector3d(1.0, 4.0, 7.0),
            Vector3d(2.0, 5.0, 8.0),
            Vector3d(3.0, 6.0, 9.0),
        )
    }

    @Test
    fun `of is an alias for ofColumnMajor`() {
        assertThat(Matrix3d.of(columnMajor)).isEqualTo(Matrix3d.ofColumnMajor(columnMajor))
    }

    @Test
    fun `toDoubleArray returns column-major order`() {
        assertThat(Matrix3d.ofColumnMajor(columnMajor).toDoubleArray()).containsExactly(*columnMajor)
    }

    @Test
    fun `equals and hashCode compare column contents`() {
        val a = Matrix3d.ofColumnMajor(columnMajor)
        val b = Matrix3d.ofColumnMajor(columnMajor.copyOf())

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
    }

    @Test
    fun `ofColumnMajor rejects array with wrong size`() {
        assertThatThrownBy { Matrix3d.ofColumnMajor(doubleArrayOf(1.0, 2.0)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 9 elements, but was 2")
    }

    @Test
    fun `ofRowMajor rejects array with wrong size`() {
        assertThatThrownBy { Matrix3d.ofRowMajor(doubleArrayOf(1.0, 2.0)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 9 elements, but was 2")
    }

    @Test
    fun `roundtrips through byte array respecting std140 column padding`() {
        val original = Matrix3d.ofColumnMajor(columnMajor)

        val restored = original.toByteArray().fromByteArray(Matrix3d::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `matches its std140 size`() {
        assertThat(Matrix3d::class.sizeOf()).isEqualTo(96)
    }
}
