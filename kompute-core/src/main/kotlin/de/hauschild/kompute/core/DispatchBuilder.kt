package de.hauschild.kompute.core

/**
 * Everything is ready for compute shader execution.
 */
interface DispatchBuilder {
    /**
     * Executes the compute shader and returns the result.
     */
    fun execute(): ShaderResult
}
