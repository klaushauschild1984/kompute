package de.hauschild.kompute.vulkan.pipeline

import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.pipeline.Pipeline
import de.hauschild.kompute.core.pipeline.Stage
import de.hauschild.kompute.core.shader.ShaderSource.Glsl.Code
import de.hauschild.kompute.vulkan.VulkanBackendExtension
import de.hauschild.kompute.vulkan.backend.VulkanBackend
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VulkanBackendExtension::class)
class VulkanPipelineTest {
    @Test
    fun `two-stage pipeline doubles values twice via an intermediate buffer`(backend: VulkanBackend) {
        val input = StorageBuffer<FloatArray>(0).data(floatArrayOf(1f, 2f, 3f))
        val intermediate = StorageBuffer<FloatArray>(1).data(floatArrayOf(0f, 0f, 0f)).asOutput()
        val output = StorageBuffer<FloatArray>(2).size(3).asOutput()

        backend.shader(Code(doubleSource(readBinding = 0, writeBinding = 1))).compile().use { shader1 ->
            backend.shader(Code(doubleSource(readBinding = 1, writeBinding = 2))).compile().use { shader2 ->
                val stage1 = Stage(shader = shader1, x = 3, data = listOf(input, intermediate))
                val stage2 = Stage(shader = shader2, x = 3, data = listOf(intermediate, output))

                Pipeline().execute(stage1, stage2).use { result ->
                    assertThat(result[output]).containsExactly(4f, 8f, 12f)
                }
            }
        }
    }

    companion object {
        private fun doubleSource(
            readBinding: Int,
            writeBinding: Int,
        ) = """
            #version 450
            layout(local_size_x = 1) in;
            layout(std430, binding = $readBinding) readonly buffer InputBuffer { float values[]; } source;
            layout(std430, binding = $writeBinding) writeonly buffer OutputBuffer { float values[]; } result;
            void main() {
                result.values[gl_GlobalInvocationID.x] = source.values[gl_GlobalInvocationID.x] * 2.0;
            }
        """.trimIndent()
    }
}
