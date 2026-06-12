package de.hauschild.kompute.core.shader

import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.result.ShaderResult

/**
 * A compiled GPU compute shader that dispatches asynchronously via Kotlin Coroutines.
 *
 * Wraps a [CompiledShader] and suspends the caller on [kotlinx.coroutines.Dispatchers.IO]
 * for the duration of the GPU dispatch. Must be closed after use to release the underlying
 * GPU program.
 *
 * Obtain an instance via the [CompiledShader.async] extension function.
 */
interface AsyncCompiledShader: AutoCloseable{
    /**
     * Dispatches the compute shader with a 1-D workgroup grid, suspending until the result
     * is available.
     *
     * Convenience for `dispatch(x, 1, 1, *data)`.
     *
     * @param x number of work groups in the X dimension — must be ≥ 1
     * @param data shader inputs and outputs — at least one output required
     * @return the results of all output buffers after the dispatch completes
     */
    suspend fun dispatch(x: Int, vararg data: ShaderData): ShaderResult = dispatch(x, 1, 1, *data)

    /**
     * Dispatches the compute shader with a 2-D workgroup grid, suspending until the result
     * is available.
     *
     * Convenience for `dispatch(x, y, 1, *data)`.
     *
     * @param x number of work groups in the X dimension — must be ≥ 1
     * @param y number of work groups in the Y dimension — must be ≥ 1
     * @param data shader inputs and outputs — at least one output required
     * @return the results of all output buffers after the dispatch completes
     */
    suspend fun dispatch(
        x: Int,
        y: Int,
        vararg data: ShaderData
    ): ShaderResult = dispatch(x, y, 1, *data)

    /**
     * Dispatches the compute shader with a 3-D workgroup grid, suspending until the result
     * is available.
     *
     * @param x number of work groups in the X dimension — must be ≥ 1
     * @param y number of work groups in the Y dimension — must be ≥ 1
     * @param z number of work groups in the Z dimension — must be ≥ 1
     * @param data shader inputs and outputs — at least one output required
     * @return the results of all output buffers after the dispatch completes
     */
    suspend fun dispatch(
        x: Int,
        y: Int,
        z: Int,
        vararg data: ShaderData
    ): ShaderResult
}
