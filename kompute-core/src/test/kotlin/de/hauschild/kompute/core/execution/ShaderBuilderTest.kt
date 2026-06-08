package de.hauschild.kompute.core.execution

import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShaderBuilderTest {
    @Test
    fun `at least one shader data is required`() {
        val builder =
            ShaderBuilder(ExecutionContext(ShaderSource.Code("glsl"))) {
                ShaderResult(emptyMap())
            }
        val exception = assertFailsWith<KomputeConfigurationException> { builder.data() }
        assertEquals("At least one data is required", exception.message)
    }

    @Test
    fun `at least one output is required`() {
        val builder =
            ShaderBuilder(ExecutionContext(ShaderSource.Code("glsl"))) {
                ShaderResult(emptyMap())
            }
        val exception =
            assertFailsWith<KomputeConfigurationException> {
                builder.data(
                    StorageBuffer<FloatArray>(0).data(floatArrayOf()),
                )
            }
        assertEquals("At least one output is required", exception.message)
    }

    @Test
    fun `outputs must be unique`() {
        val builder =
            ShaderBuilder(ExecutionContext(ShaderSource.Code("glsl"))) {
                ShaderResult(emptyMap())
            }
        val output = StorageBuffer<FloatArray>(0).size(1).asOutput()
        val exception =
            assertFailsWith<KomputeConfigurationException> {
                builder.data(
                    output,
                    output,
                )
            }
        assertEquals("There are duplicated outputs: [$output]", exception.message)
    }

    @Test
    fun `validation succeeds`() {
        val builder =
            ShaderBuilder(ExecutionContext(ShaderSource.Code("glsl"))) {
                ShaderResult(emptyMap())
            }
        builder.data(StorageBuffer<FloatArray>(0).size(128).asOutput())
    }
}
