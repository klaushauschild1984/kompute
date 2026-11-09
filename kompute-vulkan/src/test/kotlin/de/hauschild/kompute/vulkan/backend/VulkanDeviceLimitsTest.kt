package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.data.UniformBufferObject
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import de.hauschild.kompute.core.shader.ShaderSource.Glsl.Code
import de.hauschild.kompute.vulkan.VulkanTestContext
import de.hauschild.kompute.vulkan.data.VulkanImage2D
import de.hauschild.kompute.vulkan.data.VulkanUniformBufferObject
import de.hauschild.kompute.vulkan.shader.VulkanCompiledShader
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VulkanDeviceLimitsTest {
    private val testContext = VulkanTestContext()
    private val context = testContext.context

    @AfterAll
    fun tearDown() {
        testContext.close()
    }

    @Test
    fun `the physical device reports usable limits`() {
        val limits = context.limits

        assertThat(limits.maxComputeWorkGroupCountX).isPositive()
        assertThat(limits.maxComputeWorkGroupCountY).isPositive()
        assertThat(limits.maxComputeWorkGroupCountZ).isPositive()
        assertThat(limits.maxImageDimension2D).isPositive()
        assertThat(limits.maxStorageBufferRange).isPositive()
        assertThat(limits.maxUniformBufferRange).isPositive()
    }

    @Test
    fun `uniform buffer larger than the device limit is rejected`() {
        testContext.contextWith(context.limits.copy(maxUniformBufferRange = 8)).use { limited ->
            val buffer = VulkanUniformBufferObject(UniformBufferObject(0).data(ByteArray(16)), limited)

            assertThatThrownBy { buffer.bind() }
                .isInstanceOf(KomputeConfigurationException::class.java)
                .hasMessageContaining("exceeds the device limit")
        }
    }

    @Test
    fun `image larger than the device limit is rejected`() {
        testContext.contextWith(context.limits.copy(maxImageDimension2D = 4)).use { limited ->
            val image = VulkanImage2D(Image2D(0).dimension(5, 1), limited)

            assertThatThrownBy { image.bind() }
                .isInstanceOf(KomputeConfigurationException::class.java)
                .hasMessageContaining("exceeds the device limit")
        }
    }

    @Test
    fun `work group count above the device limit is rejected`() {
        val limits = context.limits.copy(
            maxComputeWorkGroupCountX = 4,
            maxComputeWorkGroupCountY = 4,
            maxComputeWorkGroupCountZ = 4,
        )
        testContext.contextWith(limits).use { limited ->
            val module = VulkanShaderModule(Code(SHADER), testContext.device)
            val shader = VulkanCompiledShader(module, VulkanPipeline(testContext.device, module), limited)
            val output = StorageBuffer<IntArray>(0).size(1).asOutput()

            shader.use {
                assertThatThrownBy { it.dispatch(5, output) }.hasMessageContaining("Work group count x")
                assertThatThrownBy { it.dispatch(1, 5, output) }.hasMessageContaining("Work group count y")
                assertThatThrownBy { it.dispatch(1, 1, 5, output) }.hasMessageContaining("Work group count z")
            }
        }
    }

    companion object {
        private val SHADER = """
            #version 450
            layout(local_size_x = 1) in;
            layout(std430, binding = 0) writeonly buffer Out { int values[]; };
            void main() { values[0] = 1; }
        """.trimIndent()
    }
}
