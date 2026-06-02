package de.hauschild.kompute.core

/**
 * Configures output data.
 */
class OutputBuilder(
    private val name: String,
    private val context: ExecutionContext,
    private val executor: (ExecutionContext) -> ShaderResult,
) {
    /**
     * Defines a buffer with the given float array.
     * @param data the float array to use for the buffer
     */
    fun buffer(data: FloatArray): ShaderBuilder {
        context.outputs[name] = data
        return ShaderBuilder(context, executor)
    }
}
