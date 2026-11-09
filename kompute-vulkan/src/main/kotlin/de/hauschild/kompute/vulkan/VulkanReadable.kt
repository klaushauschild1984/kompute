package de.hauschild.kompute.vulkan

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.vulkan.backend.VulkanTransferBatch

/**
 * Represents a Vulkan buffer that can be read back from the GPU.
 *
 * @param T the type of the value read back from the GPU
 */
interface VulkanReadable<T : Any> {
    /**
     * The source the read data belongs to.
     */
    val source: OutputCapable<T>

    /**
     * Reads the buffer contents from the GPU back to host memory in a submission of its own.
     *
     * @return the buffer contents
     */
    fun read(): T

    /**
     * Schedules reading the buffer contents in [batch], which the caller submits.
     *
     * @param batch the batch that collects the readbacks of all outputs of a dispatch
     * @return the function that converts the downloaded bytes, to be called after the batch was submitted
     */
    fun scheduleRead(batch: VulkanTransferBatch): () -> T
}
