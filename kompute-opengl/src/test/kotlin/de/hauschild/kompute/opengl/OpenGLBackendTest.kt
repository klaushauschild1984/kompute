package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.Backend
import de.hauschild.kompute.core.ShaderData.StorageBuffer
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
                ).data(
                    StorageBuffer(0).data(floatArrayOf(1f, 2f, 3f)),
                    StorageBuffer(1).size(3).asOutput("result"),
                ).dispatch(3)
                .execute()
                .storageBuffer("result")

        assertArrayEquals(floatArrayOf(1f, 2f, 3f), result)
    }
}
