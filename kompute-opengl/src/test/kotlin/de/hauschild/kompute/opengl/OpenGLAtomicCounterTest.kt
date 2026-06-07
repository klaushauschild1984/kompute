package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.execution.ShaderSource.Code
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLAtomicCounterTest {
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
            .data(atomicCounter)
            .dispatch(4)
            .execute()[atomicCounter]
        assertEquals(256, value)
    }
}
