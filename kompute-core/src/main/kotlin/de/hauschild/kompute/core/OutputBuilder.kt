package de.hauschild.kompute.core

/**
 * Configures output data.
 */
interface OutputBuilder {
    /**
     * Defines a buffer with the given float array.
     * @param data the float array to use for the buffer
     */
    fun buffer(data: FloatArray): ShaderBuilder
}
