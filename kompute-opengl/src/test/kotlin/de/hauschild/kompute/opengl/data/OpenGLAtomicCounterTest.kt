package de.hauschild.kompute.opengl.data

import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.shader.ShaderSource.Code
import de.hauschild.kompute.opengl.OpenGLBackendExtension
import de.hauschild.kompute.opengl.backend.OpenGLBackend
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLAtomicCounterTest {
    @Disabled
    @Test
    fun `atomic counter`(backend: OpenGLBackend) {
        val atomicCounter = AtomicCounter(0)

        val value = backend
            .shader(
                Code(
                    """
                    #version 430
                    layout(local_size_x = 64) in;
                    layout(binding = 0) uniform atomic_uint counter;

                    void main() {
                        atomicCounterIncrement(counter);
                    }
                    """.trimIndent()
                )
            )
            .compile()
            .use { it.dispatch(4, atomicCounter) }[atomicCounter]
        assertEquals(256, value)
    }
}
