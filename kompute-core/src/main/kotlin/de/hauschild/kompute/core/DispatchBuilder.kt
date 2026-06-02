package de.hauschild.kompute.core

/**
 * Triggers the execution of the configured compute shader.
 */
class DispatchBuilder(
    private val context: ExecutionContext,
    private val executor: (ExecutionContext) -> ShaderResult,
) {
    /**
     * Executes the compute shader and returns the result.
     */
    fun execute(): ShaderResult = executor(context)
}
