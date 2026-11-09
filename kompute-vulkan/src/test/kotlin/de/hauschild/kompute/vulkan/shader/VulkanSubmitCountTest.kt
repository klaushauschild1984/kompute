package de.hauschild.kompute.vulkan.shader

import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.data.UniformBufferObject
import de.hauschild.kompute.core.shader.ShaderSource.Glsl.Code
import de.hauschild.kompute.vulkan.VulkanTestContext
import de.hauschild.kompute.vulkan.backend.VulkanContext
import de.hauschild.kompute.vulkan.backend.VulkanPipeline
import de.hauschild.kompute.vulkan.backend.VulkanShaderModule
import de.hauschild.kompute.vulkan.backend.VulkanTransferBatch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.lwjgl.vulkan.VK10

/**
 * Checks that the uploads, the dispatch and the readbacks of a dispatch share submissions, because a
 * submission is the most expensive part of a small dispatch.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VulkanSubmitCountTest {
    private val testContext = VulkanTestContext()
    private val staging = testContext.contextWith(testContext.context.limits, directAccess = false)
    private val direct = testContext.contextWith(testContext.context.limits, directAccess = true)

    @AfterAll
    fun tearDown() {
        staging.close()
        direct.close()
        testContext.close()
    }

    private fun compile(
        glsl: String,
        context: VulkanContext,
    ): VulkanCompiledShader {
        val module = VulkanShaderModule(Code(glsl), testContext.device)
        return VulkanCompiledShader(module, VulkanPipeline(testContext.device, module), context)
    }

    private fun hasDirectAccess(): Boolean {
        val (buffer, _) = direct.bufferCache.getOrAllocate(Any(), 16, VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT)
        return buffer.isDirectlyAccessible
    }

    @Test
    fun `uploads and the dispatch share one submission, reading the result needs one more`() {
        val params = UniformBufferObject(0).data(ByteArray(Float.SIZE_BYTES))
        val image = Image2D(1).dimension(2, 2)
        val input = StorageBuffer<FloatArray>(2).data(floatArrayOf(1f))
        val output = StorageBuffer<FloatArray>(3).size(1).asOutput()

        compile(MIXED_SHADER, staging).use { shader ->
            val before = staging.commandPool.submitCount
            val result = shader.dispatch(1, params, image, input, output)
            assertThat(staging.commandPool.submitCount - before)
                .describedAs("uploads of 3 resources and the dispatch")
                .isEqualTo(1)

            result.use { it[output] }
            assertThat(staging.commandPool.submitCount - before).describedAs("including the readbacks").isEqualTo(2)
        }
    }

    @Test
    fun `a dispatch that is never read needs a single submission`() {
        val params = UniformBufferObject(0).data(ByteArray(Float.SIZE_BYTES))
        val image = Image2D(1).dimension(2, 2)
        val input = StorageBuffer<FloatArray>(2).data(floatArrayOf(1f))
        val output = StorageBuffer<FloatArray>(3).size(1).asOutput()

        compile(MIXED_SHADER, staging).use { shader ->
            val before = staging.commandPool.submitCount
            shader.dispatch(1, params, image, input, output).close()

            assertThat(staging.commandPool.submitCount - before).isEqualTo(1)
        }
    }

    @Test
    fun `several outputs are read in one submission`() {
        val first = StorageBuffer<IntArray>(0).size(1).asOutput()
        val second = StorageBuffer<IntArray>(1).size(1).asOutput()

        compile(TWO_OUTPUT_SHADER, staging).use { shader ->
            shader.dispatch(1, first, second).use { result ->
                val before = staging.commandPool.submitCount
                val values = result[first] to result[second]

                assertThat(staging.commandPool.submitCount - before).isEqualTo(1)
                assertThat(values.first).containsExactly(1)
                assertThat(values.second).containsExactly(2)
            }
        }
    }

    @Test
    fun `persistent outputs are read without a submission on a device with unified memory`() {
        assumeTrue(hasDirectAccess(), "the device has no memory the host can access directly")
        val first = StorageBuffer<IntArray>(0).size(1).asOutput()
        val second = StorageBuffer<IntArray>(1).size(1).asOutput()

        compile(TWO_OUTPUT_SHADER, direct).use { shader ->
            val before = direct.commandPool.submitCount
            shader.dispatch(1, first, second).use { result ->
                assertThat(direct.commandPool.submitCount - before).describedAs("the dispatch").isEqualTo(1)

                assertThat(result[first]).containsExactly(1)
                assertThat(result[second]).containsExactly(2)
                assertThat(direct.commandPool.submitCount - before).describedAs("including reading").isEqualTo(1)
            }
        }
    }

    @Test
    fun `an empty batch submits nothing`() {
        val before = staging.commandPool.submitCount

        VulkanTransferBatch(staging).use { it.submit() }

        assertThat(staging.commandPool.submitCount).isEqualTo(before)
    }

    companion object {
        // a uniform buffer, an image, an input and an output buffer
        private val MIXED_SHADER = """
            #version 450
            layout(local_size_x = 1) in;
            layout(std140, binding = 0) uniform Params { float factor; };
            layout(rgba8, binding = 1) writeonly uniform image2D img;
            layout(std430, binding = 2) readonly buffer In { float values[]; } inBuffer;
            layout(std430, binding = 3) writeonly buffer Out { float values[]; } outBuffer;
            void main() {
                imageStore(img, ivec2(0, 0), vec4(factor));
                outBuffer.values[0] = inBuffer.values[0];
            }
        """.trimIndent()

        // two output buffers
        private val TWO_OUTPUT_SHADER = """
            #version 450
            layout(local_size_x = 1) in;
            layout(std430, binding = 0) writeonly buffer First { int values[]; } first;
            layout(std430, binding = 1) writeonly buffer Second { int values[]; } second;
            void main() {
                first.values[0] = 1;
                second.values[0] = 2;
            }
        """.trimIndent()
    }
}
