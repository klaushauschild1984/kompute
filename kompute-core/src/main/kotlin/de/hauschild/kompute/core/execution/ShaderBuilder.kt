package de.hauschild.kompute.core.execution

import de.hauschild.kompute.core.backend.CompiledShader

/**
 * Entry point for compiling a compute shader source into a [CompiledShader].
 *
 * Created by [de.hauschild.kompute.core.backend.Backend.shader]. Call [compile] to obtain
 * a [CompiledShader] that can be dispatched multiple times without recompilation.
 *
 * ```kotlin
 * val shader = backend.shader(Code(glsl)).compile()
 * shader.use {
 *     val result = it.dispatch(64, outputBuffer)
 * }
 * ```
 *
 * @param source the GLSL source to compile
 * @param compiler backend-provided function that compiles a [ShaderSource] into a [CompiledShader]
 */
class ShaderBuilder(
    private val source: ShaderSource,
    private val compiler: (ShaderSource) -> CompiledShader
) {
    /**
     * Compiles the shader source into a [CompiledShader].
     *
     * The returned [CompiledShader] holds a compiled GPU program and must be closed after use.
     *
     * @return the compiled [CompiledShader]
     */
    fun compile() = compiler(source)
}
