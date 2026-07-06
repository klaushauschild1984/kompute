package de.hauschild.kompute.core.shader

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import kotlinx.coroutines.test.runTest

class AsyncCompiledShaderDecoratorTest {
    @Test
    fun delegation() {
        val compiledShader: CompiledShader = CompiledShaderMock()
        val asyncCompiledShader = compiledShader.async()

        runTest {
            asyncCompiledShader.dispatch(0, 0, 0).close()
            asyncCompiledShader.close()
        }

        val mock = compiledShader as CompiledShaderMock
        assertThat(mock.dispatched).isTrue()
        assertThat(mock.closed).isTrue()
    }
}
