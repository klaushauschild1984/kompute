package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class StorageBufferTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource
    fun `validation succeeds`(
        buffer: StorageBuffer<*>,
    ) {
        buffer.validate()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    fun `validation fails`(
        buffer: StorageBuffer<*>,
    ) {
        assertThatThrownBy { buffer.validate() }.isInstanceOf(KomputeConfigurationException::class.java)
    }

    @Test
    fun `mode resolves to Input when only data is set`() {
        val buffer = StorageBuffer<FloatArray>(0).data(floatArrayOf(1f, 2f, 3f))

        val mode = buffer.mode()

        assertThat(mode).isInstanceOf(StorageBuffer.Mode.Input::class.java)
        assertThat((mode as StorageBuffer.Mode.Input<FloatArray>).data).containsExactly(1f, 2f, 3f)
    }

    @Test
    fun `mode resolves to Output when only size and asOutput are set`() {
        val buffer = StorageBuffer<FloatArray>(0).size(4).asOutput()

        assertThat(buffer.mode()).isEqualTo(StorageBuffer.Mode.Output<FloatArray>(4))
    }

    @Test
    fun `mode resolves to ReadWrite when data and asOutput are set`() {
        val buffer = StorageBuffer<FloatArray>(0).data(floatArrayOf(1f, 2f, 3f)).asOutput()

        val mode = buffer.mode()

        assertThat(mode).isInstanceOf(StorageBuffer.Mode.ReadWrite::class.java)
        assertThat((mode as StorageBuffer.Mode.ReadWrite<FloatArray>).data).containsExactly(1f, 2f, 3f)
    }

    companion object {
        @JvmStatic
        fun `validation succeeds`() =
            listOf(
                StorageBuffer<FloatArray>(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)),
                StorageBuffer<FloatArray>(0).size(4).asOutput(),
                StorageBuffer<FloatArray>(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)).asOutput(),
            )

        @JvmStatic
        fun `validation fails`() =
            listOf(
                StorageBuffer<Any>(-1),
                StorageBuffer<FloatArray>(-1),
                StorageBuffer<Any>(0),
                StorageBuffer<FloatArray>(0),
                StorageBuffer<FloatArray>(0).size(4),
                StorageBuffer<FloatArray>(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)).size(4),
            )
    }
}
