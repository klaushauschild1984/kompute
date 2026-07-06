package de.hauschild.kompute.core.shader

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExtensionTest {
    @Test
    fun async() {
        val compiledShader: CompiledShader = CompiledShaderMock()
        val asyncCompiledShader = compiledShader.async()
        assertThat(asyncCompiledShader).isInstanceOf(AsyncCompiledShader::class.java)
    }
}
