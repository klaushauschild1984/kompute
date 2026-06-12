package de.hauschild.kompute.core.shader

import org.junit.jupiter.api.Test

import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AsyncCompiledShaderDecoratorTest {
    @Test
    fun delegation() {
        val compiledShader: CompiledShader = CompiledShaderMock()
        val asyncCompiledShader = compiledShader.async()

        runTest {
            asyncCompiledShader.dispatch(0, 0, 0)
            asyncCompiledShader.close()
        }

        assertTrue { (compiledShader as CompiledShaderMock).dispatched }
        assertTrue { (compiledShader as CompiledShaderMock).closed }
    }
}
