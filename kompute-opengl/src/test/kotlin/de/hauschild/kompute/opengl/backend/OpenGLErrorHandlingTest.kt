package de.hauschild.kompute.opengl.backend

import de.hauschild.kompute.core.exception.KomputeBackendInitializationException
import de.hauschild.kompute.core.shader.ShaderSource.Glsl.Code
import de.hauschild.kompute.opengl.OpenGLBackendExtension
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLErrorHandlingTest {
    @Test
    fun `invalid shader source fails to compile`(backend: OpenGLBackend) {
        assertThatThrownBy { backend.shader(Code(INVALID_SOURCE)).compile() }
            .isInstanceOf(KomputeBackendInitializationException::class.java)
            .hasMessageStartingWith("Shader compile error:")
    }

    @Test
    fun `closing a compiled shader twice is safe`(backend: OpenGLBackend) {
        val compiledShader = backend.shader(Code(VALID_SOURCE)).compile()

        compiledShader.close()
        compiledShader.close()
    }

    companion object {
        private val INVALID_SOURCE = """
            #version 430 core
            this is not valid GLSL
        """.trimIndent()
        private val VALID_SOURCE = """
            #version 430 core
            layout(local_size_x = 1) in;
            void main() {}
        """.trimIndent()
    }
}
