package de.hauschild.kompute.vulkan.shader

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.result.ResultReader
import de.hauschild.kompute.vulkan.VulkanBindable
import de.hauschild.kompute.vulkan.VulkanReadable

/**
 * Reads all output resources of a dispatch back from the GPU.
 *
 * The bound resources are released as soon as the results were read - or on [close] if they never
 * were. Both are safe to call repeatedly.
 *
 * @param bindables the resources bound for the dispatch
 */
internal class VulkanResultReader(
    private val bindables: List<VulkanBindable>,
) : ResultReader {
    override fun read(): Map<OutputCapable<*>, Any> = try {
        bindables.filterIsInstance<VulkanReadable<*>>()
            .filter { readable -> readable.source.isOutput }
            .associate { readable -> readable.source to readable.read() }
    } finally {
        close()
    }

    override fun close() {
        bindables.forEach { it.close() }
    }
}
