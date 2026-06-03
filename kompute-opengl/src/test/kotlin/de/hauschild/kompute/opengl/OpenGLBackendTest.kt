package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.Backend
import de.hauschild.kompute.core.ShaderSource.Stream
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertIs

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLBackendTest {
    @Test
    fun `OpenGL Backend Test`(backend: Backend) {
        assertIs<OpenGLBackend>(backend)
    }

    @Test
    fun `Copy Shader (3 elements)`(backend: Backend) {
        val result =
            backend
                .shader(
                    Stream(
                        OpenGLBackendTest::class.java
                            .getResourceAsStream("copy.glsl")!!,
                    ),
                ).input(0)
                .buffer(floatArrayOf(1f, 2f, 3f))
                .output(1, "result")
                .buffer(FloatArray(3))
                .dispatch(3)
                .execute()
                .output("result")

        assertArrayEquals(floatArrayOf(1f, 2f, 3f), result)
    }
}
