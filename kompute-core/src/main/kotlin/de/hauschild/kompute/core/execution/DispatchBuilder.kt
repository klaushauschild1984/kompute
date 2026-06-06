package de.hauschild.kompute.core.execution

import de.hauschild.kompute.core.exception.requireConfiguration

/**
 * Configures the dispatch dimensions for the compute shader execution.
 *
 * The dimensions define how many workgroups are launched in each direction (x, y, z).
 * The total number of shader invocations equals the workgroup count multiplied by the
 * local workgroup size declared in the shader source (`local_size_x/y/z`).
 *
 * For 1D computations use only x. For 2D use x and y. For 3D specify all three.
 *
 * @param context
 * @param executor
 */
class DispatchBuilder(
    private val context: ExecutionContext,
    private val executor: (ExecutionContext) -> ShaderResult,
) {
    /**
     * Dispatches the shader with the given workgroup grid dimensions.
     *
     * @param x number of workgroups in the x dimension
     * @param y number of workgroups in the y dimension (defaults to 1)
     * @param z number of workgroups in the z dimension (defaults to 1)
     * @return an [ExecutionBuilder] to trigger shader execution
     * @throws [KomputeConfigurationException] if one of the work group count are less than zero
     */
    fun dispatch(
        x: Int,
        y: Int = 1,
        z: Int = 1,
    ): ExecutionBuilder {
        val workGroupCountValidationMessage = { "Work group count must be greater than or equal to one" }
        requireConfiguration(x >= 1, workGroupCountValidationMessage)
        context.x = x
        requireConfiguration(y >= 1, workGroupCountValidationMessage)
        context.y = y
        requireConfiguration(z >= 1, workGroupCountValidationMessage)
        context.z = z
        return ExecutionBuilder(context, executor)
    }
}
