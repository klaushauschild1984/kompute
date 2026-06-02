package de.hauschild.kompute.core

/**
 * Represents a compute backend capable of executing GPU shaders.
 *
 * Backends are obtained via [Kompute] and should be used with [use] to ensure proper resource cleanup.
 */
interface Backend : AutoCloseable {
    /**
     * Will be used by [Kompute] to determine the type of the backend.
     */
    @InternalApi
    fun type(): Type

    /**
     * Initializes the backend, preparing it for use.
     */
    @InternalApi
    fun initialize()

    /**
     * Specifies the compute shader to be used for computations.
     *
     * @param source the source of the compute shader
     */
    fun shader(source: ShaderSource): ShaderBuilder
}
