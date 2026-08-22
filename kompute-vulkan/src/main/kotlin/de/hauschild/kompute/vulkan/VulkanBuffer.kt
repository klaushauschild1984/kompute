package de.hauschild.kompute.vulkan

import de.hauschild.kompute.core.data.IndexedBinding
import de.hauschild.kompute.core.data.ShaderData

/**
 * Wraps a Vulkan buffer resource for a [ShaderData] + [IndexedBinding] configuration.
 *
 * @param T the [ShaderData] + [IndexedBinding] type this buffer wraps
 * @property source the shader data configuration this buffer is based on
 */
abstract class VulkanBuffer<T>(
    val source: T,
) : VulkanBindable
where T : ShaderData, T : IndexedBinding {
    abstract override fun close()
}
