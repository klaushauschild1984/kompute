package de.hauschild.kompute.vulkan.shader

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.result.ResultReader
import de.hauschild.kompute.vulkan.VulkanBindable
import de.hauschild.kompute.vulkan.VulkanReadable
import de.hauschild.kompute.vulkan.backend.VulkanContext
import de.hauschild.kompute.vulkan.backend.VulkanTransferBatch

/**
 * Reads all output resources of a dispatch back from the GPU, in a single submission.
 *
 * The bound resources are released as soon as the results were read - or on [close] if they never
 * were. Both are safe to call repeatedly. Until then the reader is tracked by the registry of the
 * [context], which releases it when the backend is closed.
 *
 * @param bindables the resources bound for the dispatch
 * @param context the [VulkanContext] providing the registry and the queue
 */
internal class VulkanResultReader(
    private val bindables: List<VulkanBindable>,
    private val context: VulkanContext,
) : ResultReader {
    init {
        context.resources.register(this)
    }

    override fun read(): Map<OutputCapable<*>, Any> = try {
        val outputs = bindables.filterIsInstance<VulkanReadable<*>>().filter { readable -> readable.source.isOutput }
        VulkanTransferBatch(context).use { batch ->
            val pending = outputs.associate { readable -> readable.source to readable.scheduleRead(batch) }
            batch.submit()
            pending.mapValues { (_, convert) -> convert() }
        }
    } finally {
        close()
    }

    override fun close() {
        context.resources.unregister(this)
        bindables.forEach { it.close() }
    }
}
