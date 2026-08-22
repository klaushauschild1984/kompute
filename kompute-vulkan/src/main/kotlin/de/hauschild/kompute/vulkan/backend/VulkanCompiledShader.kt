package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.data.IndexedBinding
import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.data.UniformBufferObject
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import de.hauschild.kompute.core.exception.requireConfiguration
import de.hauschild.kompute.core.result.ShaderResult
import de.hauschild.kompute.core.shader.AbstractCompiledShader
import org.lwjgl.vulkan.VK10

/**
 * A compiled Vulkan compute shader, backed by a [VulkanShaderModule] and its [VulkanPipeline].
 *
 * @param vulkanShaderModule the compiled [VulkanShaderModule]
 * @param vulkanPipeline the [VulkanPipeline] built from [vulkanShaderModule]
 */
class VulkanCompiledShader(
    private val vulkanShaderModule: VulkanShaderModule,
    private val vulkanPipeline: VulkanPipeline,
) : AbstractCompiledShader() {
    /**
     * Validates [data] against [AbstractCompiledShader], then checks that the supplied bindings
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

        val suppliedByIndex = data.filterIsInstance<IndexedBinding>().associateBy { it.index }
        val reflectedByIndex = vulkanShaderModule.bindings.associateBy { it.binding }

        requireConfiguration(suppliedByIndex.keys == reflectedByIndex.keys) {
            "ShaderData bindings ${suppliedByIndex.keys} do not match the shader's declared bindings " +
                "${reflectedByIndex.keys}"
        }

        reflectedByIndex.forEach { (index, reflected) ->
            val supplied = suppliedByIndex.getValue(index)
            val actualDescriptorType = descriptorTypeOf(supplied)
            requireConfiguration(actualDescriptorType == reflected.descriptorType) {
                "Binding $index: shader declares ${DESCRIPTOR_TYPE_NAMES[reflected.descriptorType]}, " +
                    "but ${supplied::class.simpleName} was provided"
            }
        }
    }

    private fun descriptorTypeOf(binding: IndexedBinding): Int = when (binding) {
        is StorageBuffer<*> -> VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER
        is UniformBufferObject -> VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
        is Image2D -> VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE
        else -> throw KomputeConfigurationException(
            "${binding::class.simpleName} is not supported by the Vulkan backend",
        )
    }

    override fun doDispatch(
        x: Int,
        y: Int,
        z: Int,
        data: List<ShaderData>
    ): ShaderResult {
        TODO("Not yet implemented")
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
