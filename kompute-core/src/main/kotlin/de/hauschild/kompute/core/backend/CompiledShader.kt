package de.hauschild.kompute.core.backend

import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.execution.ShaderResult

/**
 * A compiled GPU compute shader that can be dispatched multiple times without recompilation.
 *
 * Obtained via [de.hauschild.kompute.core.execution.ShaderBuilder.compile]. Must be closed
 * after use to release the underlying GPU program.
 *
 * The three [dispatch] overloads cover 1-D, 2-D, and 3-D workgroup grids — dimensions not
 * specified default to 1.
 */
interface CompiledShader : AutoCloseable {
    /**
     * Dispatches the compute shader with a 1-D workgroup grid.
     *
     * Convenience for `dispatch(x, 1, 1, *data)`.
     *
     * @param x number of work groups in the X dimension — must be ≥ 1
     * @param data shader inputs and outputs — at least one output required
     * @return the results of all output buffers after the dispatch completes
     */
    fun dispatch(
        x: Int,
        vararg data: ShaderData
    ): ShaderResult = dispatch(x, 1,1,*data)

    /**
     * Dispatches the compute shader with a 2-D workgroup grid.
     *
     * Convenience for `dispatch(x, y, 1, *data)`.
     *
     * @param x number of work groups in the X dimension — must be ≥ 1
     * @param y number of work groups in the Y dimension — must be ≥ 1
     * @param data shader inputs and outputs — at least one output required
     * @return the results of all output buffers after the dispatch completes
     */
    fun dispatch(
        x: Int,
        y: Int,
        vararg data: ShaderData
    ): ShaderResult = dispatch(x, y,1,*data)

    /**
     * Dispatches the compute shader with a 3-D workgroup grid.
     *
     * @param x number of work groups in the X dimension — must be ≥ 1
     * @param y number of work groups in the Y dimension — must be ≥ 1
     * @param z number of work groups in the Z dimension — must be ≥ 1
     * @param data shader inputs and outputs — at least one output required
     * @return the results of all output buffers after the dispatch completes
     */
    fun dispatch(
        x: Int,
        y: Int,
        z: Int,
        vararg data: ShaderData
    ): ShaderResult
}
