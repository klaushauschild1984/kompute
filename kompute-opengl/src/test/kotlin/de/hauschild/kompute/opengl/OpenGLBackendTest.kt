package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.Backend
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertIs

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLBackendTest {
    @Test
    fun `OpenGL Backend Test`(backend: Backend) {
        assertIs<OpenGLBackend>(backend)
    }
}
