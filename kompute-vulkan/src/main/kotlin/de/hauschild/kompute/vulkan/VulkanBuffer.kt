package de.hauschild.kompute.vulkan

import de.hauschild.kompute.core.data.IndexedBinding
import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.vulkan.backend.VulkanContext
import de.hauschild.kompute.vulkan.backend.VulkanTransferBatch
import de.hauschild.kompute.vulkan.backend.submitBind

/**
 * Wraps a Vulkan buffer resource for a [ShaderData] + [IndexedBinding] configuration.
 *
 * @param T the [ShaderData] + [IndexedBinding] type this buffer wraps
 * @property source the shader data configuration this buffer is based on
 * @property context the [VulkanContext] used for memory allocation and transfer commands
 */
abstract class VulkanBuffer<T>(
    val source: T,
    protected val context: VulkanContext,
) : VulkanBindable
where T : ShaderData, T : IndexedBinding {
    override fun bind() = context.submitBind { batch -> scheduleBind(batch) }

    abstract override fun scheduleBind(batch: VulkanTransferBatch)

    abstract override fun close()
}
