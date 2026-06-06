package de.hauschild.kompute.opengl

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
