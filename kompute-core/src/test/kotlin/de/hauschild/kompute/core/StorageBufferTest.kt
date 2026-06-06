package de.hauschild.kompute.core

import de.hauschild.kompute.core.ShaderData.StorageBuffer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class StorageBufferTest {
    @ParameterizedTest(name = "{2}")
    @MethodSource
    @Suppress("UnusedParameter")
    fun `local validation`(
        buffer: StorageBuffer<*>,
        validExpected: Boolean,
        description: String,
    ) {
        if (validExpected) {
            buffer.validate()
        } else {
            assertFailsWith<KomputeConfigurationException> { buffer.validate() }
        }
    }

    companion object {
        @JvmStatic
        fun `local validation`() =
            listOf(
                Arguments.of(
                    StorageBuffer<FloatArray>(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)),
                    true,
                    "data only, input",
                ),
                Arguments.of(
                    StorageBuffer<FloatArray>(0).size(4).asOutput(),
                    true,
                    "size only, output",
                ),
                Arguments.of(
                    StorageBuffer<FloatArray>(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)).asOutput(),
                    true,
                    "data + name, input/output",
                ),
                Arguments.of(
                    StorageBuffer<Any>(-1),
                    false,
                    "unsupported data type",
                ),
                Arguments.of(
                    StorageBuffer<FloatArray>(-1),
                    false,
                    "index less than 0",
                ),
                Arguments.of(
                    StorageBuffer<FloatArray>(0),
                    false,
                    "data and size missing",
                ),
                Arguments.of(
                    StorageBuffer<FloatArray>(0).size(4),
                    false,
                    "output name missing",
                ),
                Arguments.of(
                    StorageBuffer<FloatArray>(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)).size(4),
                    false,
                    "data and size",
                ),
            )
    }
}
