package de.hauschild.kompute.core

import de.hauschild.kompute.core.ShaderData.OutputCapable

/**
 * Holds the output data from a compute shader execution.
 */
class ShaderResult(
    private val outputs: Map<OutputCapable<*>, Any>,
) {
    /**
     * Retrieves the output data for the given [OutputCapable].
     * It is required that it was marked as output beforehand, so that [OutputCapable.isOutput] is true.
     *
     * @return the data for the particular [OutputCapable]
     * @throws KomputeConfigurationException if no output is available for the given [OutputCapable]
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(output: OutputCapable<T>): T =
        (outputs[output] ?: throw KomputeConfigurationException("No output available for $output")) as T
}
