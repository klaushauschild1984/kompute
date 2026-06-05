package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.Backend
import de.hauschild.kompute.core.ShaderSource.Stream
import de.hauschild.kompute.core.StorageBuffer
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
        val output = StorageBuffer<FloatArray>(1).size(3).asOutput()
        val result =
            backend
                .shader(
                    Stream(
                        OpenGLBackendTest::class.java
                            .getResourceAsStream("copy.glsl")!!,
                    ),
                ).data(
                    StorageBuffer<FloatArray>(0).data(floatArrayOf(1f, 2f, 3f)),
                    output,
                ).dispatch(3)
                .execute()[output]

        assertArrayEquals(floatArrayOf(1f, 2f, 3f), result)
    }
}
