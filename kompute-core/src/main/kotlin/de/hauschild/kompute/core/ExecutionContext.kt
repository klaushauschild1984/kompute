package de.hauschild.kompute.core

/**
 * Holds the mutable configuration state for a compute shader execution.
 *
 * Carries the shader source, input/output data, and dispatch dimensions through the shader
 * configuration pipeline ([ShaderBuilder] → [DispatchBuilder] → [ExecutionBuilder]).
 *
 * Created automatically by [AbstractBackend.shader] — not intended for direct use.
 *
 * @property source The source of the compute shader to execute.
 */
class ExecutionContext(
    val source: ShaderSource,
) {
    /**
     * Shader data (storage buffers) attached to this computation. Populated by [ShaderBuilder.data].
     */
    val data = mutableListOf<ShaderData>()

    /**
     * Number of workgroups in the x dimension. Set by [DispatchBuilder.dispatch]. Defaults to 1.
     */
    var x = 1

    /**
     * Number of workgroups in the y dimension. Set by [DispatchBuilder.dispatch]. Defaults to 1.
     */
    var y = 1

    /**
     * Number of workgroups in the z dimension. Set by [DispatchBuilder.dispatch]. Defaults to 1.
     */
    var z = 1
}
