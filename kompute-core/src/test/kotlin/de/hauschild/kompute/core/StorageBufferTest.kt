package de.hauschild.kompute.core

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class StorageBufferTest {
    @ParameterizedTest(name = "{2}")
    @MethodSource
    fun validate(
        buffer: ShaderData.StorageBuffer,
        validExpected: Boolean,
        description: String,
    ) {
        if (validExpected) {
            buffer.validate()
        } else {
            assertFailsWith<IllegalArgumentException> { buffer.validate() }
        }
    }

    companion object {
        @JvmStatic
        fun validate() =
            listOf(
                Arguments.of(
                    ShaderData.StorageBuffer(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)),
                    true,
                    "data only, input",
                ),
                Arguments.of(
                    ShaderData.StorageBuffer(0).size(4).asOutput("output"),
                    true,
                    "size only, output",
                ),
                Arguments.of(
                    ShaderData.StorageBuffer(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)).asOutput("output"),
                    true,
                    "data + name, input/output",
                ),
                Arguments.of(
                    ShaderData.StorageBuffer(-1),
                    false,
                    "index less than 0",
                ),
                Arguments.of(
                    ShaderData.StorageBuffer(0),
                    false,
                    "data and size missing",
                ),
                Arguments.of(
                    ShaderData.StorageBuffer(0).size(4),
                    false,
                    "output name missing",
                ),
                Arguments.of(
                    ShaderData.StorageBuffer(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)).size(4),
                    false,
                    "data and size",
                ),
            )
    }
}
