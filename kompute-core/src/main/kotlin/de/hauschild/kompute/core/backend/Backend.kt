package de.hauschild.kompute.core.backend

import de.hauschild.kompute.core.InternalApi
import de.hauschild.kompute.core.execution.ShaderBuilder
import de.hauschild.kompute.core.execution.ShaderSource

/**
 * Represents a compute backend capable of executing GPU shaders.
 *
 * Backends are obtained via [de.hauschild.kompute.core.Kompute] and should be used with [use] to ensure proper resource cleanup.
 */
interface Backend : AutoCloseable {
    /**
     * Will be used by [de.hauschild.kompute.core.Kompute] to determine the type of the backend.
     *
     * @return the [Type] of the backend
     */
    @InternalApi
    fun type(): Type

    /**
     * Initializes the backend, preparing it for use.
     *
     * @throws [KomputeBackendInitializationException] if backend initialization fails
     */
    @InternalApi
    fun initialize()

    /**
     * Specifies the compute shader to be used for computations.
     *
     * @param source the source of the compute shader
     * @return a [ShaderBuilder] for configuring shader data
     */
    fun shader(source: ShaderSource): ShaderBuilder
}
