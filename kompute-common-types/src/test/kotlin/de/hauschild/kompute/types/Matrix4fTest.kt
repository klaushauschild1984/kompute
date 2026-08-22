package de.hauschild.kompute.types

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class Matrix4fTest {
    private val columnMajor = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f)

    @Test
    fun `ofColumnMajor slices the array into columns directly`() {
        val matrix = Matrix4f.ofColumnMajor(columnMajor)

        assertThat(matrix.columns).containsExactly(
            Vector4f(1f, 2f, 3f, 4f),
            Vector4f(5f, 6f, 7f, 8f),
            Vector4f(9f, 10f, 11f, 12f),
            Vector4f(13f, 14f, 15f, 16f),
        )
    }

    @Test
    fun `ofRowMajor transposes rows into columns`() {
        val matrix = Matrix4f.ofRowMajor(columnMajor)

        assertThat(matrix.columns).containsExactly(
            Vector4f(1f, 5f, 9f, 13f),
            Vector4f(2f, 6f, 10f, 14f),
            Vector4f(3f, 7f, 11f, 15f),
            Vector4f(4f, 8f, 12f, 16f),
        )
    }

    @Test
    fun `of is an alias for ofColumnMajor`() {
        assertThat(Matrix4f.of(columnMajor)).isEqualTo(Matrix4f.ofColumnMajor(columnMajor))
    }

    @Test
    fun `toFloatArray returns column-major order`() {
        assertThat(Matrix4f.ofColumnMajor(columnMajor).toFloatArray()).containsExactly(*columnMajor)
    }

    @Test
    fun `equals and hashCode compare column contents`() {
        val a = Matrix4f.ofColumnMajor(columnMajor)
        val b = Matrix4f.ofColumnMajor(columnMajor.copyOf())

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
    }

    @Test
    fun `ofColumnMajor rejects array with wrong size`() {
        assertThatThrownBy { Matrix4f.ofColumnMajor(floatArrayOf(1f, 2f)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 16 elements, but was 2")
    }

    @Test
    fun `ofRowMajor rejects array with wrong size`() {
        assertThatThrownBy { Matrix4f.ofRowMajor(floatArrayOf(1f, 2f)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Expected 16 elements, but was 2")
    }

    @Test
    fun `roundtrips through byte array`() {
        val original = Matrix4f.ofColumnMajor(columnMajor)

        val restored = original.toByteArray().fromByteArray(Matrix4f::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `matches its std140 size`() {
        assertThat(Matrix4f::class.sizeOf()).isEqualTo(64)
    }
}
