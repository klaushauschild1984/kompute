package de.hauschild.kompute.core

/**
 * Configures input data.
 */
class InputBuilder(
    private val name: String,
    private val context: ExecutionContext,
    private val executor: (ExecutionContext) -> ShaderResult,
) {
    /**
     * Defines a buffer with the given float array.
     * @param data the float array to use for the buffer
     */
    fun buffer(data: FloatArray): ShaderBuilder {
        context.inputs[name] = data
        return ShaderBuilder(context, executor)
    }
}
