package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.requireBackendInitialization
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkComputePipelineCreateInfo
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo

/**
 * Wrapper around the Vulkan descriptor set layout, pipeline layout, and compute pipeline for a
 * single [VulkanShaderModule].
 *
 * The descriptor set layout is built from [VulkanShaderModule.bindings], reflected from the
 * shader's SPIR-V bytecode - the shader itself is the source of truth for its binding layout.
 *
 * @param device the [VulkanDevice]
 * @param shaderModule the compiled [VulkanShaderModule]
 */
@Suppress("TooGenericExceptionCaught") // cleans up on any failure and rethrows
class VulkanPipeline(
    private val device: VulkanDevice,
    shaderModule: VulkanShaderModule,
) : AutoCloseable {
    private var descriptorSetLayout: Long = VK10.VK_NULL_HANDLE
    private var pipelineLayout: Long = VK10.VK_NULL_HANDLE
    private var pipeline: Long = VK10.VK_NULL_HANDLE

    init {
        try {
            descriptorSetLayout = createDescriptorSetLayout(shaderModule.bindings)
            pipelineLayout = createPipelineLayout(descriptorSetLayout)
            pipeline = createPipeline(shaderModule.handle(), pipelineLayout)
        } catch (t: Throwable) {
            close()
            throw t
        }
    }

    private fun createDescriptorSetLayout(bindings: List<VulkanShaderBinding>): Long =
        MemoryStack.stackPush().use { stack ->
            val layoutBindings = VkDescriptorSetLayoutBinding.calloc(bindings.size, stack)
            bindings.forEachIndexed { index, binding ->
                layoutBindings[index]
                    .binding(binding.binding)
                    .descriptorType(binding.descriptorType)
                    .descriptorCount(1)
                    .stageFlags(VK10.VK_SHADER_STAGE_COMPUTE_BIT)
            }
            val createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(layoutBindings)
            val pointer = stack.mallocLong(1)
            val result = VK10.vkCreateDescriptorSetLayout(device.device, createInfo, null, pointer)
            requireBackendInitialization(result == VK10.VK_SUCCESS) {
                "Failed to create Vulkan descriptor set layout: ${vkResultName(result)}"
            }
            pointer.get(0)
        }

    private fun createPipelineLayout(descriptorSetLayout: Long): Long =
        MemoryStack.stackPush().use { stack ->
            val createInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pSetLayouts(stack.longs(descriptorSetLayout))
            val pointer = stack.mallocLong(1)
            val result = VK10.vkCreatePipelineLayout(device.device, createInfo, null, pointer)
            requireBackendInitialization(result == VK10.VK_SUCCESS) {
                "Failed to create Vulkan pipeline layout: ${vkResultName(result)}"
            }
            pointer.get(0)
        }

    private fun createPipeline(shaderModuleHandle: Long, pipelineLayout: Long): Long =
        MemoryStack.stackPush().use { stack ->
            val stageCreateInfo = VkPipelineShaderStageCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(VK10.VK_SHADER_STAGE_COMPUTE_BIT)
                .module(shaderModuleHandle)
                .pName(stack.UTF8("main"))
            val createInfo = VkComputePipelineCreateInfo.calloc(1, stack)
                .sType(VK10.VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
                .stage(stageCreateInfo)
                .layout(pipelineLayout)
            val pointer = stack.mallocLong(1)
            val result = VK10.vkCreateComputePipelines(
                device.device, VK10.VK_NULL_HANDLE, createInfo, null, pointer,
            )
            requireBackendInitialization(result == VK10.VK_SUCCESS) {
                "Failed to create Vulkan compute pipeline: ${vkResultName(result)}"
            }
            pointer.get(0)
        }

    override fun close() {
        if (pipeline != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyPipeline(device.device, pipeline, null)
        }
        if (pipelineLayout != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyPipelineLayout(device.device, pipelineLayout, null)
        }
        if (descriptorSetLayout != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyDescriptorSetLayout(device.device, descriptorSetLayout, null)
        }
    }
}
