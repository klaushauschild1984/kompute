package de.hauschild.kompute.vulkan.shader

import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.data.IndexedBinding
import de.hauschild.kompute.core.data.NamedUniform
import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.data.UniformBufferObject
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import de.hauschild.kompute.core.exception.requireConfiguration
import de.hauschild.kompute.core.result.ShaderResult
import de.hauschild.kompute.core.shader.AbstractCompiledShader
import de.hauschild.kompute.vulkan.VulkanBindable
import de.hauschild.kompute.vulkan.backend.VulkanContext
import de.hauschild.kompute.vulkan.backend.VulkanDescriptorSet
import de.hauschild.kompute.vulkan.backend.VulkanPipeline
import de.hauschild.kompute.vulkan.backend.VulkanShaderModule
import de.hauschild.kompute.vulkan.backend.VulkanTimeoutException
import de.hauschild.kompute.vulkan.backend.VulkanTransferBatch
import de.hauschild.kompute.vulkan.data.VulkanAtomicCounter
import de.hauschild.kompute.vulkan.data.VulkanImage2D
import de.hauschild.kompute.vulkan.data.VulkanStorageBuffer
import de.hauschild.kompute.vulkan.data.VulkanUniformBufferObject
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkCommandBuffer

/**
 * A compiled Vulkan compute shader, backed by a [VulkanShaderModule] and its [VulkanPipeline].
 *
 * @param vulkanShaderModule the compiled [VulkanShaderModule]
 * @param vulkanPipeline the [VulkanPipeline] built from [vulkanShaderModule]
 * @param context the [VulkanContext] handed to the buffers created for a dispatch
 */
class VulkanCompiledShader(
    private val vulkanShaderModule: VulkanShaderModule,
    private val vulkanPipeline: VulkanPipeline,
    private val context: VulkanContext,
) : AbstractCompiledShader() {
    /**
     * Validates the work group counts against the device limits and [data] against
     * [AbstractCompiledShader], then checks that the supplied bindings
     * match exactly - by index and by descriptor type - what [vulkanShaderModule] reflected from
     * the shader's SPIR-V bytecode. The shader is the source of truth for its binding layout.
     *
     * @throws KomputeConfigurationException if a binding is missing, unexpected, or of the wrong type
     */
    override fun validateDispatch(
        x: Int,
        y: Int,
        z: Int,
        vararg data: ShaderData
    ) {
        super.validateDispatch(x, y, z, *data)
        val limits = context.limits
        requireConfiguration(x <= limits.maxComputeWorkGroupCountX) {
            "Work group count x must not exceed physical limit ${limits.maxComputeWorkGroupCountX}"
        }
        requireConfiguration(y <= limits.maxComputeWorkGroupCountY) {
            "Work group count y must not exceed physical limit ${limits.maxComputeWorkGroupCountY}"
        }
        requireConfiguration(z <= limits.maxComputeWorkGroupCountZ) {
            "Work group count z must not exceed physical limit ${limits.maxComputeWorkGroupCountZ}"
        }

        val suppliedByIndex = data.filterIsInstance<IndexedBinding>().associateBy { it.index }
        val reflectedByIndex = vulkanShaderModule.bindings.associateBy { it.binding }

        requireConfiguration(suppliedByIndex.keys == reflectedByIndex.keys) {
            "ShaderData bindings ${suppliedByIndex.keys} do not match the shader's bindings ${reflectedByIndex.keys}"
        }

        reflectedByIndex.forEach { (index, reflected) ->
            val supplied = suppliedByIndex.getValue(index)
            val actualDescriptorType = descriptorTypeOf(supplied)
            val declared = DESCRIPTOR_TYPE_NAMES[reflected.descriptorType]
            val provided = supplied::class.simpleName
            requireConfiguration(actualDescriptorType == reflected.descriptorType) {
                "Binding $index: shader declares $declared, but $provided was provided"
            }
        }
    }

    private fun descriptorTypeOf(binding: IndexedBinding): Int = when (binding) {
        is StorageBuffer<*> -> VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER
        is AtomicCounter -> VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER
        is UniformBufferObject -> VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
        is Image2D -> VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE
        else -> throw KomputeConfigurationException(
            "${binding::class.simpleName} is not supported by the Vulkan backend",
        )
    }

    @Suppress("TooGenericExceptionCaught") // releases the bound resources on any failure and rethrows
    override fun doDispatch(
        x: Int,
        y: Int,
        z: Int,
        data: List<ShaderData>
    ): ShaderResult {
        val bindables = mutableListOf<VulkanBindable>()
        try {
            data.mapTo(bindables) { shaderData -> createBindable(shaderData) }
            dispatch(x, y, z, bindables)
        } catch (t: Throwable) {
            releaseAfterFailure(bindables, t)
            throw t
        }
        val reader = VulkanResultReader(bindables, context)
        return ShaderResult(reader).also { context.resources.closeWhenUnreachable(it, reader) }
    }

    /**
     * Binds all resources, which schedules their uploads, and runs the uploads and the dispatch in a
     * single submission.
     */
    private fun dispatch(
        x: Int,
        y: Int,
        z: Int,
        bindables: List<VulkanBindable>,
    ) {
        VulkanTransferBatch(context).use { batch ->
            bindables.forEach { it.scheduleBind(batch) }
            val descriptorSet = if (bindables.isEmpty()) {
                null
            } else {
                VulkanDescriptorSet(context.device, vulkanPipeline.descriptorSetLayout, vulkanShaderModule.bindings)
            }
            try {
                descriptorSet?.update(bindables)
                batch.submit { commandBuffer ->
                    VK10.vkCmdBindPipeline(commandBuffer, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, vulkanPipeline.pipeline)
                    descriptorSet?.let { bindDescriptorSet(commandBuffer, it) }
                    VK10.vkCmdDispatch(commandBuffer, x, y, z)
                }
            } catch (timeout: VulkanTimeoutException) {
                // the GPU may still use the descriptor set, it is released with the backend
                descriptorSet?.let { context.resources.register(it) }
                throw timeout
            }
            descriptorSet?.close()
        }
    }

    private fun createBindable(shaderData: ShaderData): VulkanBindable = when (shaderData) {
        is StorageBuffer<*> -> VulkanStorageBuffer(shaderData, context)
        is UniformBufferObject -> VulkanUniformBufferObject(shaderData, context)
        is Image2D -> VulkanImage2D(shaderData, context)
        is AtomicCounter -> VulkanAtomicCounter(shaderData, context)
        is NamedUniform<*> -> throw KomputeConfigurationException(
            "${shaderData::class.simpleName} is not supported by the Vulkan backend",
        )
    }

    private fun bindDescriptorSet(
        commandBuffer: VkCommandBuffer,
        descriptorSet: VulkanDescriptorSet,
    ) {
        MemoryStack.stackPush().use { stack ->
            VK10.vkCmdBindDescriptorSets(
                commandBuffer,
                VK10.VK_PIPELINE_BIND_POINT_COMPUTE,
                vulkanPipeline.pipelineLayout,
                0,
                stack.longs(descriptorSet.handle),
                null,
            )
        }
    }

    private fun releaseAfterFailure(
        bindables: List<VulkanBindable>,
        failure: Throwable,
    ) {
        if (failure is VulkanTimeoutException) {
            // the GPU may still use the resources, they are released with the backend
            context.resources.register { bindables.forEach { it.close() } }
        } else {
            bindables.forEach { runCatching { it.close() }.onFailure(failure::addSuppressed) }
        }
    }

    override fun close() {
        vulkanPipeline.close()
        vulkanShaderModule.close()
    }

    companion object {
        private val DESCRIPTOR_TYPE_NAMES = mapOf(
            VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER to "StorageBuffer",
            VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER to "UniformBufferObject",
            VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE to "Image2D",
        )
    }
}
