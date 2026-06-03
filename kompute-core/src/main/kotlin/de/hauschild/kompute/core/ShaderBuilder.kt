package de.hauschild.kompute.core

/**
 * Attaches input and output data to a compute shader.
 *
 * Each [ShaderData] is self-validated before being accepted. Proceed to [DispatchBuilder]
 * after configuring all data.
 */
class ShaderBuilder(
    private val context: ExecutionContext,
    private val executor: (ExecutionContext) -> ShaderResult,
) {
    /**
     * Attaches one or more shader data objects to this computation.
     *
     * Each item is validated immediately via [ShaderData.validate].
     *
     * @param data the shader data to attach (storage buffers, etc.)
     * @return a [DispatchBuilder] to configure the compute grid dimensions
     * @throws IllegalArgumentException if any item fails validation
     */
    fun data(vararg data: ShaderData): DispatchBuilder {
        data.forEach { it.validate() }
        context.data.addAll(data.toList())
        return DispatchBuilder(context, executor)
    }
}
