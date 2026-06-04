package de.hauschild.kompute.core

import de.hauschild.kompute.core.ShaderData.StorageBuffer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StorageBufferTest {
    @ParameterizedTest(name = "{2}")
    @MethodSource
    @Suppress("UnusedParameter")
    fun `local validation`(
        buffer: StorageBuffer,
        validExpected: Boolean,
        description: String,
    ) {
        if (validExpected) {
            buffer.validate()
        } else {
            assertFailsWith<IllegalArgumentException> { buffer.validate() }
        }
    }

    @Test
    fun `cross validation`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                StorageBuffer.crossValidate(
                    listOf(
                        StorageBuffer(0),
                        StorageBuffer(0),
                    ),
                )
            }
        assertEquals("There are duplicated indices: [0]", exception.message)
    }

    companion object {
        @JvmStatic
        fun `local validation`() =
            listOf(
                Arguments.of(
                    StorageBuffer(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)),
                    true,
                    "data only, input",
                ),
                Arguments.of(
                    StorageBuffer(0).size(4).asOutput("output"),
                    true,
                    "size only, output",
                ),
                Arguments.of(
                    StorageBuffer(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)).asOutput("output"),
                    true,
                    "data + name, input/output",
                ),
                Arguments.of(
                    StorageBuffer(-1),
                    false,
                    "index less than 0",
                ),
                Arguments.of(
                    StorageBuffer(0),
                    false,
                    "data and size missing",
                ),
                Arguments.of(
                    StorageBuffer(0).size(4),
                    false,
                    "output name missing",
                ),
                Arguments.of(
                    StorageBuffer(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)).size(4),
                    false,
                    "data and size",
                ),
            )
    }
}
