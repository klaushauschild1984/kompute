package de.hauschild.kompute.core

/**
 * Configures input data.
 */
interface InputBuilder {
    /**
     * Defines a buffer with the given float array.
     * @param data the float array to use for the buffer
     */
    fun buffer(data: FloatArray): ShaderBuilder
}
