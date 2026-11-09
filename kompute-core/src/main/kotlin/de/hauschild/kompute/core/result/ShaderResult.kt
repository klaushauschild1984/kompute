package de.hauschild.kompute.core.result

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.exception.KomputeConfigurationException

/**
 * Holds the output data from a compute shader execution.
 *
 * @param reader a [ResultReader] that reads the output data from the GPU
 */
class ShaderResult(
    private val reader: ResultReader,
) : AutoCloseable {
    private val outputsLazy = lazy { reader.read() }
    private val outputs by outputsLazy

    /**
     * Retrieves the output data for the given [OutputCapable].
     * It is required that it was marked as output beforehand, so that [OutputCapable.isOutput] is true.
     *
     * @param output the output buffer to retrieve the result for
     * @return the data for the particular [OutputCapable]
     * @throws KomputeConfigurationException if no output is available for the given [OutputCapable]
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(output: OutputCapable<T>): T =
        (outputs[output] ?: throw KomputeConfigurationException("No output available for $output")) as T

    override fun close() {
        if (!outputsLazy.isInitialized()) {
            reader.close()
        }
    }
}
