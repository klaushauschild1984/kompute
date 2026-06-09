package de.hauschild.kompute.core.backend

import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import de.hauschild.kompute.core.execution.ShaderResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AbstractCompiledShaderTest {
    private val shader = TestCompiledShader()

    @Test
    fun `work group count must be at least one`() {
        val output = StorageBuffer<FloatArray>(0).size(1).asOutput()
        val exception = assertFailsWith<KomputeConfigurationException> { shader.dispatch(0, output) }
        assertEquals("Work group count must be greater than or equal to one", exception.message)
    }

    @Test
    fun `at least one data is required`() {
        val exception = assertFailsWith<KomputeConfigurationException> { shader.dispatch(1) }
        assertEquals("At least one data is required", exception.message)
    }

    @Test
    fun `at least one output is required`() {
        val exception = assertFailsWith<KomputeConfigurationException> {
            shader.dispatch(1, StorageBuffer<FloatArray>(0).data(floatArrayOf()))
        }
        assertEquals("At least one output is required", exception.message)
    }

    @Test
    fun `outputs must be unique`() {
        val output = StorageBuffer<FloatArray>(0).size(1).asOutput()
        val exception = assertFailsWith<KomputeConfigurationException> {
            shader.dispatch(1, output, output)
        }
        assertEquals("There are duplicated outputs: [$output]", exception.message)
    }

    @Test
    fun `validation succeeds`() {
        shader.dispatch(1, StorageBuffer<FloatArray>(0).size(1).asOutput())
    }

    private class TestCompiledShader : AbstractCompiledShader() {
        override fun dispatch(
            x: Int,
            y: Int,
            z: Int,
            vararg data: ShaderData
        ): ShaderResult {
            validateDispatch(x, y, z, *data)
            return ShaderResult(emptyMap())
        }

        override fun close() {
            // nothing to do
        }
    }
}
