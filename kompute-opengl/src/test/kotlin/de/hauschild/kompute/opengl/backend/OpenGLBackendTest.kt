package de.hauschild.kompute.opengl.backend

import de.hauschild.kompute.opengl.OpenGLBackendExtension
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertIs

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLBackendTest {
    @Test
    fun `backend initialization`(backend: OpenGLBackend) {
        assertIs<OpenGLBackend>(backend)
    }
}
