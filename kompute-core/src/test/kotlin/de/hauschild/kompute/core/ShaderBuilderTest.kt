package de.hauschild.kompute.core

import de.hauschild.kompute.core.ShaderData.StorageBuffer
import de.hauschild.kompute.core.ShaderSource.Code
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShaderBuilderTest {
    @Test
    fun `at least one shader data is required`() {
        val builder =
            ShaderBuilder(ExecutionContext(Code(""))) {
                ShaderResult(emptyMap())
            }
        val exception = assertFailsWith<KomputeConfigurationException> { builder.data() }
        assertEquals("At least one data is required", exception.message)
    }

    @Test
    fun `at least one output is required`() {
        val builder =
            ShaderBuilder(ExecutionContext(Code(""))) {
                ShaderResult(emptyMap())
            }
        val exception =
            assertFailsWith<KomputeConfigurationException> { builder.data(StorageBuffer(0).data(floatArrayOf())) }
        assertEquals("At least one output is required", exception.message)
    }

    @Test
    fun `output names must be unique`() {
        val builder =
            ShaderBuilder(ExecutionContext(Code(""))) {
                ShaderResult(emptyMap())
            }
        val exception =
            assertFailsWith<KomputeConfigurationException> {
                builder.data(
                    StorageBuffer(0).size(1).asOutput("output"),
                    StorageBuffer(1).size(1).asOutput("output"),
                )
            }
        assertEquals("There are duplicated output names: [output]", exception.message)
    }

    @Test
    fun `validation succeeds`() {
        val builder =
            ShaderBuilder(ExecutionContext(Code(""))) {
                ShaderResult(emptyMap())
            }
        builder.data(StorageBuffer(0).size(128).asOutput("output"))
    }
}
