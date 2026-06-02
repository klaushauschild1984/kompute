package de.hauschild.kompute.core

/**
 * Assembles input and output variables for a shader, and defines dispatch parameters.
 *
 * The names of input and output parameters must match the binding names declared in the shader source,
 * otherwise an [IllegalStateException] is thrown during [DispatchBuilder.execute].
 */
class ShaderBuilder(
    private val context: ExecutionContext,
    private val executor: (ExecutionContext) -> ShaderResult,
) {
    /**
     * Specify an input parameter with the given name.
     */
    fun input(name: String): InputBuilder = InputBuilder(name, context, executor)

    /**
     * Specify an output parameter with the given name.
     */
    fun output(name: String): OutputBuilder = OutputBuilder(name, context, executor)

    /**
     * Define the dispatch parameters for the shader.
     */
    fun dispatch(
        x: Int,
        y: Int = 1,
        z: Int = 1,
    ): DispatchBuilder {
        context.x = x
        context.y = y
        context.z = z
        return DispatchBuilder(context, executor)
    }
}
