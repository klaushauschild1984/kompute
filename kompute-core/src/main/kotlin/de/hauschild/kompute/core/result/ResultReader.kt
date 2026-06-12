package de.hauschild.kompute.core.result

import de.hauschild.kompute.core.data.OutputCapable

/**
 * Interface for reading the output data from a compute shader execution.
 */
fun interface ResultReader : AutoCloseable {
    /**
     * Reads the output data from the GPU.
     *
     * @return a map of [OutputCapable] to the data read from the GPU
     */
    fun read(): Map<OutputCapable<*>, Any>

    override fun close() = Unit
}
