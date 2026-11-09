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
import org.lwjgl.vulkan.VK10.vkFreeCommandBuffers
import org.lwjgl.vulkan.VK10.vkQueueSubmit
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
 */
class VulkanCommandPool(
    private val vulkanDevice: VulkanDevice,
    physicalDevice: VulkanPhysicalDevice,
) : AutoCloseable {
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
     * until the GPU has finished executing it.
     *
     * Access to the command pool and the queue is externally synchronized by this method; all
     * submissions to the compute queue must go through the pool.
     *
     * @param record records the commands into the given command buffer
     * @throws KomputeBackendDispatchException if recording, submission or waiting fails
     */
    @Synchronized
    fun executeOneTime(record: (VkCommandBuffer) -> Unit) {
        MemoryStack.stackPush().use { stack ->
            val allocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool)
                .level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1)
            val commandBufferPointer = stack.mallocPointer(1)
            checkVk(vkAllocateCommandBuffers(vulkanDevice.device, allocateInfo, commandBufferPointer)) {
                "Failed to allocate Vulkan command buffer"
            }
            val commandBuffer = VkCommandBuffer(commandBufferPointer.get(0), vulkanDevice.device)
            var fence = VK10.VK_NULL_HANDLE
            try {
                val beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
                checkVk(vkBeginCommandBuffer(commandBuffer, beginInfo)) { "Failed to begin command buffer" }
                record(commandBuffer)
                checkVk(vkEndCommandBuffer(commandBuffer)) { "Failed to end command buffer" }

                val fenceInfo = VkFenceCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                val fencePointer = stack.mallocLong(1)
                checkVk(vkCreateFence(vulkanDevice.device, fenceInfo, null, fencePointer)) {
                    "Failed to create fence"
                }
                fence = fencePointer.get(0)

                val submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(commandBuffer))
                checkVk(vkQueueSubmit(vulkanDevice.queue, submitInfo, fence)) { "Failed to submit command buffer" }
                checkVk(vkWaitForFences(vulkanDevice.device, stack.longs(fence), true, Long.MAX_VALUE)) {
                    "Failed to wait for command buffer completion"
                }
            } finally {
                if (fence != VK10.VK_NULL_HANDLE) {
                    vkDestroyFence(vulkanDevice.device, fence, null)
                }
                vkFreeCommandBuffers(vulkanDevice.device, commandPool, commandBuffer)
            }
        }
    }

    override fun close() {
        vkDestroyCommandPool(vulkanDevice.device, commandPool, null)
    }
}
