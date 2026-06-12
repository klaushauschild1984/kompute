package de.hauschild.kompute.opengl.pipeline

import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.pipeline.Pipeline
import de.hauschild.kompute.core.pipeline.Stage
import de.hauschild.kompute.core.shader.ShaderSource.Code
import de.hauschild.kompute.opengl.OpenGLBackendExtension
import de.hauschild.kompute.opengl.backend.OpenGLBackend
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLPipelineTest {
    @Test
    fun `two-stage pipeline doubles values twice via intermediate buffer`(backend: OpenGLBackend) {
        val input = StorageBuffer<FloatArray>(0).data(floatArrayOf(1f, 2f, 3f))
        val intermediate = StorageBuffer<FloatArray>(1).data(floatArrayOf(0f, 0f, 0f)).asOutput()
        val output = StorageBuffer<FloatArray>(2).size(3).asOutput()

        val stage1 = Stage(
            shader = backend.shader(Code(doubleSource(readBinding = 0, writeBinding = 1))).compile(),
            x = 3,
            data = listOf(input, intermediate),
        )
        val stage2 = Stage(
            shader = backend.shader(Code(doubleSource(readBinding = 1, writeBinding = 2))).compile(),
            x = 3,
            data = listOf(intermediate, output),
        )

        Pipeline().execute(stage1, stage2).use { result ->
            assertArrayEquals(floatArrayOf(4f, 8f, 12f), result[output], 0.001f)
        }
    }

    companion object {
        private fun doubleSource(readBinding: Int, writeBinding: Int) = """
            #version 430 core

            layout (local_size_x = 1) in;

            layout (std430, binding = $readBinding) readonly buffer InputBuffer {
                float values[];
            } source;

            layout (std430, binding = $writeBinding) writeonly buffer OutputBuffer {
                float values[];
            } result;

            void main() {
                result.values[gl_GlobalInvocationID.x] = source.values[gl_GlobalInvocationID.x] * 2.0;
            }
        """.trimIndent()
    }
}
