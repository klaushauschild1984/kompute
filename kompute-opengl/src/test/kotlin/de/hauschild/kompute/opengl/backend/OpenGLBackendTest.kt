package de.hauschild.kompute.opengl.backend

import de.hauschild.kompute.opengl.OpenGLBackendExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLBackendTest {
    @Test
    fun `backend initialization`(backend: OpenGLBackend) {
        assertThat(backend).isInstanceOf(OpenGLBackend::class.java)
    }
}
