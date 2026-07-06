package de.hauschild.kompute.core.shader

import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import de.hauschild.kompute.core.result.ShaderResult
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AbstractCompiledShaderTest {
    private val shader = TestCompiledShader()

    @Test
    fun `work group count must be at least one`() {
        val output = StorageBuffer.Companion<FloatArray>(0).size(1).asOutput()
        assertThatThrownBy { shader.dispatch(0, output) }
            .isInstanceOf(KomputeConfigurationException::class.java)
            .hasMessage("Work group count must be greater than or equal to one")
    }

    @Test
    fun `at least one data is required`() {
        assertThatThrownBy { shader.dispatch(1) }
            .isInstanceOf(KomputeConfigurationException::class.java)
            .hasMessage("At least one data is required")
    }

    @Test
    fun `at least one output is required`() {
        assertThatThrownBy {
            shader.dispatch(1, StorageBuffer.Companion<FloatArray>(0).data(floatArrayOf()))
        }
            .isInstanceOf(KomputeConfigurationException::class.java)
            .hasMessage("At least one output is required")
    }

    @Test
    fun `outputs must be unique`() {
        val output = StorageBuffer.Companion<FloatArray>(0).size(1).asOutput()
        assertThatThrownBy { shader.dispatch(1, output, output) }
            .isInstanceOf(KomputeConfigurationException::class.java)
            .hasMessage("There are duplicated outputs: [$output]")
    }

    @Test
    fun `validation succeeds`() {
        shader.dispatch(1, StorageBuffer.Companion<FloatArray>(0).size(1).asOutput()).close()
    }

    private class TestCompiledShader : AbstractCompiledShader() {
        override fun doDispatch(
            x: Int,
            y: Int,
            z: Int,
            data: List<ShaderData>
        ): ShaderResult = ShaderResult { emptyMap() }

        override fun close() {
            // nothing to do
        }
    }
}
