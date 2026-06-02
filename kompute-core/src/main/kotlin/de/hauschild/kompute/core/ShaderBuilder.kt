package de.hauschild.kompute.core

/**
 * Assembles input and output variables for a shader, and defines dispatch parameters.
 *
 * The names of input and output parameters must match the binding names declared in the shader source,
 * otherwise an [IllegalStateException] is thrown during [DispatchBuilder.execute].
 */
interface ShaderBuilder {
    /**
     * Specify an input parameter with the given name.
     */
    fun input(name: String): InputBuilder

    /**
     * Specify an output parameter with the given name.
     */
    fun output(name: String): OutputBuilder

    /**
     * Define the dispatch parameters for the shader.
     */
    fun dispatch(
        x: Int,
        y: Int = 1,
        z: Int = 1,
    ): DispatchBuilder
}
