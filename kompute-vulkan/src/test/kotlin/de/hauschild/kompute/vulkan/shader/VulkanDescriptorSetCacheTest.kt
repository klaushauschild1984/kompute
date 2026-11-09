package de.hauschild.kompute.vulkan.shader

import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.data.UniformBufferObject
import de.hauschild.kompute.core.shader.ShaderSource.Glsl.Code
import de.hauschild.kompute.vulkan.VulkanTestContext
import de.hauschild.kompute.vulkan.backend.VulkanPipeline
import de.hauschild.kompute.vulkan.backend.VulkanShaderModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VulkanDescriptorSetCacheTest {
    private val testContext = VulkanTestContext()

    @AfterAll
    fun tearDown() {
        testContext.close()
    }

    private fun compile(glsl: String): VulkanCompiledShader {
        val module = VulkanShaderModule(Code(glsl), testContext.device)
        return VulkanCompiledShader(module, VulkanPipeline(testContext.device, module), testContext.context)
    }

    @Test
    fun `dispatches with the same persistent buffers share one descriptor set`() {
        val input = StorageBuffer<IntArray>(0).data(intArrayOf(1, 2, 3))
        val output = StorageBuffer<IntArray>(1).size(3).asOutput()

        compile(COPY_SHADER).use { shader ->
            repeat(5) {
                assertThat(shader.dispatch(3, input, output).use { result -> result[output] }).containsExactly(1, 2, 3)
            }

            assertThat(shader.descriptorSets.createdCount).isEqualTo(1)
            assertThat(shader.descriptorSets.cachedCount()).isEqualTo(1)
        }
    }

    @Test
    fun `transient resources get a descriptor set per dispatch and nothing is cached`() {
        val params = UniformBufferObject(0).data(ByteArray(Int.SIZE_BYTES))
        val output = StorageBuffer<IntArray>(1).size(1).asOutput()

        compile(UNIFORM_SHADER).use { shader ->
            repeat(3) { shader.dispatch(1, params, output).use { result -> result[output] } }

            assertThat(shader.descriptorSets.createdCount).isEqualTo(3)
            assertThat(shader.descriptorSets.cachedCount()).isZero()
        }
    }

    @Test
    fun `a replaced buffer gets a new descriptor set and the stale one is released`() {
        val input = StorageBuffer<IntArray>(0).data(intArrayOf(1, 2, 3, 4, 5))
        val output = StorageBuffer<IntArray>(1).size(3).asOutput()

        compile(COPY_SHADER).use { shader ->
            shader.dispatch(3, input, output).use { result -> result[output] }
            output.size(5)
            val resized = shader.dispatch(5, input, output).use { result -> result[output] }

            assertThat(resized).containsExactly(1, 2, 3, 4, 5)
            assertThat(shader.descriptorSets.createdCount).isEqualTo(2)
            assertThat(shader.descriptorSets.cachedCount()).isEqualTo(1)
        }
    }

    @Test
    fun `at most eight descriptor sets are kept`() {
        compile(COPY_SHADER).use { shader ->
            repeat(10) { round ->
                val input = StorageBuffer<IntArray>(0).data(intArrayOf(round))
                val output = StorageBuffer<IntArray>(1).size(1).asOutput()

                assertThat(shader.dispatch(1, input, output).use { result -> result[output] }).containsExactly(round)
            }

            assertThat(shader.descriptorSets.createdCount).isEqualTo(10)
            assertThat(shader.descriptorSets.cachedCount()).isEqualTo(MAX_CACHED)
        }
    }

    @Test
    fun `closing the shader releases the cached descriptor sets`() {
        val input = StorageBuffer<IntArray>(0).data(intArrayOf(1))
        val output = StorageBuffer<IntArray>(1).size(1).asOutput()
        val shader = compile(COPY_SHADER)
        shader.dispatch(1, input, output).use { result -> result[output] }

        shader.close()

        assertThat(shader.descriptorSets.cachedCount()).isZero()
    }

    companion object {
        private const val MAX_CACHED = 8

        // copies the input buffer to the output buffer
        private val COPY_SHADER = """
            #version 450
            layout(local_size_x = 1) in;
            layout(std430, binding = 0) readonly buffer In { int values[]; } inBuffer;
            layout(std430, binding = 1) writeonly buffer Out { int values[]; } outBuffer;
            void main() { outBuffer.values[gl_GlobalInvocationID.x] = inBuffer.values[gl_GlobalInvocationID.x]; }
        """.trimIndent()

        // reads a uniform buffer and writes to an output buffer
        private val UNIFORM_SHADER = """
            #version 450
            layout(local_size_x = 1) in;
            layout(std140, binding = 0) uniform Params { int factor; };
            layout(std430, binding = 1) writeonly buffer Out { int values[]; } outBuffer;
            void main() { outBuffer.values[0] = factor; }
        """.trimIndent()
    }
}
