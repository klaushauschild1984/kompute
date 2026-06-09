package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.execution.ShaderSource.Code
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLImage2DTest {
    @Test
    fun image2D(backend: OpenGLBackend) {
        val image2D = Image2D(0).dimension(2, 2)

        val bytes = backend.shader(
            Code(
                """
                #version 430
                layout(local_size_x = 1, local_size_y = 1) in;
                layout(binding = 0, rgba8) uniform writeonly image2D outputImage;

                void main() {
                  imageStore(outputImage, ivec2(gl_GlobalInvocationID.xy), vec4(1.0, 0.0, 0.0, 1.0));
                }
                """.trimIndent()
            )
        ).compile()
            .use { it.dispatch(2, 2, image2D) }[image2D].data

        assertEquals(16, bytes.size)
        assertContentEquals(ByteArray(16) {
            byteArrayOf(255.toByte(), 0, 0, 255.toByte())[it % 4]
        }, bytes)
    }
}
