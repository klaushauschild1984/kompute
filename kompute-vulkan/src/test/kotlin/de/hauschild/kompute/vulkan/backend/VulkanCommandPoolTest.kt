package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.KomputeBackendDispatchException
import de.hauschild.kompute.vulkan.VulkanTestContext
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkFenceCreateInfo

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VulkanCommandPoolTest {
    private val testContext = VulkanTestContext()

    @AfterAll
    fun tearDown() {
        testContext.close()
    }

    @Test
    fun `a fence that is not signaled in time raises a timeout and makes the pool unusable`() {
        testContext.commandPoolWithTimeout(ONE_MILLISECOND).use { pool ->
            MemoryStack.stackPush().use { stack ->
                val fence = createFence(0, stack)

                assertThatThrownBy { pool.waitForFence(fence, stack) }
                    .isInstanceOf(VulkanTimeoutException::class.java)
                    .hasMessageContaining("kompute.backend.vulkan.timeout")
                VK10.vkDestroyFence(testContext.device.device, fence, null)
            }

            assertThatThrownBy { pool.executeOneTime { Unit } }
                .isInstanceOf(KomputeBackendDispatchException::class.java)
                .hasMessageContaining("unusable")
        }
    }

    @Test
    fun `a signaled fence is passed without a timeout`() {
        testContext.commandPoolWithTimeout(ONE_MILLISECOND).use { pool ->
            MemoryStack.stackPush().use { stack ->
                val fence = createFence(VK10.VK_FENCE_CREATE_SIGNALED_BIT, stack)

                assertThatCode { pool.waitForFence(fence, stack) }.doesNotThrowAnyException()
                VK10.vkDestroyFence(testContext.device.device, fence, null)
            }

            assertThatCode { pool.executeOneTime { Unit } }.doesNotThrowAnyException()
        }
    }

    private fun createFence(
        flags: Int,
        stack: MemoryStack,
    ): Long {
        val fenceInfo = VkFenceCreateInfo.calloc(stack)
            .sType(VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
            .flags(flags)
        val fencePointer = stack.mallocLong(1)
        val result = VK10.vkCreateFence(testContext.device.device, fenceInfo, null, fencePointer)
        checkVk(result) { "Failed to create fence" }
        return fencePointer.get(0)
    }

    companion object {
        private const val ONE_MILLISECOND = 1_000_000L
    }
}
