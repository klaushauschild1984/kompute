package de.hauschild.kompute.core.execution

/**
 * Triggers the execution of the configured compute shader on the GPU.
 *
 * This is the final step in the shader pipeline after data and dispatch dimensions
 * have been configured via [ShaderBuilder] and [DispatchBuilder].
 *
 * @param context
 * @param executor
 */
class ExecutionBuilder(
    private val context: ExecutionContext,
    private val executor: (ExecutionContext) -> ShaderResult,
) {
    /**
     * Executes the compute shader synchronously and returns the results.
     *
     * Blocks until GPU computation is complete. All storage buffers marked via
     * [StorageBuffer.asOutput] are available in the returned [ShaderResult].
     *
     * @return a [ShaderResult] containing all output storage buffers
     */
    fun execute(): ShaderResult = executor(context)
}
