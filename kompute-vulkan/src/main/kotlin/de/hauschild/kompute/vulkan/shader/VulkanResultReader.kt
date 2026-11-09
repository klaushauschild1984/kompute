package de.hauschild.kompute.vulkan.shader

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.result.ResultReader
import de.hauschild.kompute.vulkan.VulkanBindable
import de.hauschild.kompute.vulkan.VulkanReadable
import de.hauschild.kompute.vulkan.backend.VulkanResourceRegistry

/**
 * Reads all output resources of a dispatch back from the GPU.
 *
 * The bound resources are released as soon as the results were read - or on [close] if they never
 * were. Both are safe to call repeatedly. Until then the reader is tracked by the [registry], which
 * releases it when the backend is closed.
 *
 * @param bindables the resources bound for the dispatch
 * @param registry the registry that tracks the reader while it holds resources
 */
internal class VulkanResultReader(
    private val bindables: List<VulkanBindable>,
    private val registry: VulkanResourceRegistry,
) : ResultReader {
    init {
        registry.register(this)
    }

    override fun read(): Map<OutputCapable<*>, Any> = try {
        bindables.filterIsInstance<VulkanReadable<*>>()
            .filter { readable -> readable.source.isOutput }
            .associate { readable -> readable.source to readable.read() }
    } finally {
        close()
    }

    override fun close() {
        registry.unregister(this)
        bindables.forEach { it.close() }
    }
}
