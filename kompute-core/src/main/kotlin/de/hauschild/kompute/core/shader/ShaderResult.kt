package de.hauschild.kompute.core.shader

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.exception.KomputeConfigurationException

/**
 * Holds the output data from a compute shader execution.
 *
 * @param outputs map of [OutputCapable] shader data to their results after dispatch
 */
class ShaderResult(
    private val outputs: Map<OutputCapable<*>, Any>,
) {
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
}
