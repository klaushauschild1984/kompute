package de.hauschild.kompute.core.shader

import kotlin.test.Test
import kotlin.test.assertIs

class ExtensionTest {
    @Test
    fun async() {
        val compiledShader: CompiledShader = CompiledShaderMock()
        val asyncCompiledShader = compiledShader.async()
        assertIs<AsyncCompiledShader>(asyncCompiledShader)
    }
}
