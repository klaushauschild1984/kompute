package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.KomputeBackendDispatchException
import de.hauschild.kompute.core.exception.requireBackendInitialization
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers
import org.lwjgl.vulkan.VK10.vkBeginCommandBuffer
import org.lwjgl.vulkan.VK10.vkCreateCommandPool
import org.lwjgl.vulkan.VK10.vkCreateFence
import org.lwjgl.vulkan.VK10.vkDestroyCommandPool
import org.lwjgl.vulkan.VK10.vkDestroyFence
import org.lwjgl.vulkan.VK10.vkEndCommandBuffer
import org.lwjgl.vulkan.VK10.vkQueueSubmit
import org.lwjgl.vulkan.VK10.vkResetCommandBuffer
import org.lwjgl.vulkan.VK10.vkResetFences
import org.lwjgl.vulkan.VK10.vkWaitForFences
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo
import org.lwjgl.vulkan.VkCommandBufferBeginInfo
import org.lwjgl.vulkan.VkCommandPoolCreateInfo
import org.lwjgl.vulkan.VkFenceCreateInfo
import org.lwjgl.vulkan.VkSubmitInfo

/**
 * Wrapper around the Vulkan command pool for the compute queue family.
 *
 * @param vulkanDevice the [VulkanDevice]
 * @param physicalDevice the [VulkanPhysicalDevice]
 * @param fenceTimeoutNanos how long to wait for a submission in nanoseconds, [NO_TIMEOUT] waits forever
 */
class VulkanCommandPool(
    private val vulkanDevice: VulkanDevice,
    physicalDevice: VulkanPhysicalDevice,
    private val fenceTimeoutNanos: Long = NO_TIMEOUT,
) : AutoCloseable {
    private var faulted = false
    private var reusableCommandBuffer: VkCommandBuffer? = null
    private var reusableFence: Long = VK10.VK_NULL_HANDLE

    /**
     * The raw command pool handle (non-dispatchable, hence a plain [Long]).
     */
    val commandPool: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val createInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(physicalDevice.queueFamilyIndex)

            val commandPoolPointer = stack.mallocLong(1)
            val result = vkCreateCommandPool(vulkanDevice.device, createInfo, null, commandPoolPointer)
            requireBackendInitialization(result == VK10.VK_SUCCESS) {
                "Failed to create Vulkan command pool: ${vkResultName(result)}"
            }
            commandPool = commandPoolPointer.get(0)
        }
    }

    /**
     * Records a primary command buffer via [record], submits it to the compute queue and blocks
     * until the GPU has finished executing it. The command buffer and the fence are reused by all
     * submissions, which is possible because each one has finished before the next one starts.
     *
     * Access to the command pool and the queue is externally synchronized by this method; all
     * submissions to the compute queue must go through the pool.
     *
     * @param record records the commands into the given command buffer
     * @throws KomputeBackendDispatchException if recording, submission or waiting fails, or if the pool is
     * unusable after an earlier timeout
     * @throws VulkanTimeoutException if the GPU did not finish within the timeout
     */
    @Synchronized
    fun executeOneTime(record: (VkCommandBuffer) -> Unit) {
        if (faulted) {
            throw KomputeBackendDispatchException("The Vulkan backend is unusable after a timeout, close it")
        }
        MemoryStack.stackPush().use { stack ->
            val commandBuffer = reusableCommandBuffer
                ?: allocateCommandBuffer(stack).also { reusableCommandBuffer = it }
            val fence = reusableFence.takeIf { it != VK10.VK_NULL_HANDLE }
                ?: createFence(stack).also { reusableFence = it }

            checkVk(vkResetCommandBuffer(commandBuffer, 0)) { "Failed to reset command buffer" }
            val beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
            checkVk(vkBeginCommandBuffer(commandBuffer, beginInfo)) { "Failed to begin command buffer" }
            record(commandBuffer)
            checkVk(vkEndCommandBuffer(commandBuffer)) { "Failed to end command buffer" }

            checkVk(vkResetFences(vulkanDevice.device, stack.longs(fence))) { "Failed to reset fence" }
            val submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(stack.pointers(commandBuffer))
            checkVk(vkQueueSubmit(vulkanDevice.queue, submitInfo, fence)) { "Failed to submit command buffer" }
            waitForFence(fence, stack)
        }
    }

    private fun allocateCommandBuffer(stack: MemoryStack): VkCommandBuffer {
        val allocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
            .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
            .commandPool(commandPool)
            .level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            .commandBufferCount(1)
        val commandBufferPointer = stack.mallocPointer(1)
        checkVk(vkAllocateCommandBuffers(vulkanDevice.device, allocateInfo, commandBufferPointer)) {
            "Failed to allocate Vulkan command buffer"
        }
        return VkCommandBuffer(commandBufferPointer.get(0), vulkanDevice.device)
    }

    private fun createFence(stack: MemoryStack): Long {
        val fenceInfo = VkFenceCreateInfo.calloc(stack)
            .sType(VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
        val fencePointer = stack.mallocLong(1)
        checkVk(vkCreateFence(vulkanDevice.device, fenceInfo, null, fencePointer)) { "Failed to create fence" }
        return fencePointer.get(0)
    }

    /**
     * Waits for [fence] and marks the pool as unusable if the timeout elapses.
     *
     * @param fence the raw fence handle of a submission
     * @param stack the stack to allocate the fence array on
     * @throws VulkanTimeoutException if the fence was not signaled within the timeout
     */
    internal fun waitForFence(
        fence: Long,
        stack: MemoryStack,
    ) {
        val result = vkWaitForFences(vulkanDevice.device, stack.longs(fence), true, fenceTimeoutNanos)
        if (result == VK10.VK_TIMEOUT) {
            faulted = true
            val seconds = fenceTimeoutNanos / NANOS_PER_SECOND
            throw VulkanTimeoutException(
                "A GPU submission did not finish within $seconds s (kompute.backend.vulkan.timeout), close the backend",
            )
        }
        checkVk(result) { "Failed to wait for command buffer completion" }
    }

    override fun close() {
        if (reusableFence != VK10.VK_NULL_HANDLE) {
            vkDestroyFence(vulkanDevice.device, reusableFence, null)
            reusableFence = VK10.VK_NULL_HANDLE
        }
        // destroying the pool frees the reusable command buffer
        vkDestroyCommandPool(vulkanDevice.device, commandPool, null)
    }

    companion object {
        /**
         * Waits for a submission without a time limit.
         */
        const val NO_TIMEOUT = Long.MAX_VALUE

        /**
         * The number of nanoseconds in one second.
         */
        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
