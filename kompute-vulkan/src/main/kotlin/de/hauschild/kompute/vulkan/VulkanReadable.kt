package de.hauschild.kompute.vulkan

import de.hauschild.kompute.core.data.OutputCapable

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
     * Reads the buffer contents from the GPU back to host memory.
     */
    fun read(): T
}
