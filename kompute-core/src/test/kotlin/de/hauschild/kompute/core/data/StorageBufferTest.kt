package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

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
        assertFailsWith<KomputeConfigurationException> { buffer.validate() }
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
                StorageBuffer<FloatArray>(0),
                StorageBuffer<FloatArray>(0).size(4),
                StorageBuffer<FloatArray>(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)).size(4),
            )
    }
}
