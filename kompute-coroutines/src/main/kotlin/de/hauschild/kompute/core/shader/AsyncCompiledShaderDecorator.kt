package de.hauschild.kompute.core.shader

import de.hauschild.kompute.core.data.ShaderData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decorator for [CompiledShader] that dispatches shader executions on a background thread.
 *
 * @param delegate the [CompiledShader] to decorate and delegate to
 * @param dispatcher the [CoroutineDispatcher] shader executions are dispatched on - defaults to [Dispatchers.IO]
 */
internal class AsyncCompiledShaderDecorator(
    private val delegate: CompiledShader,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
): AsyncCompiledShader{
    override suspend fun dispatch(
        x: Int,
        y: Int,
        z: Int,
        vararg data: ShaderData
    ) =
        withContext(dispatcher) { delegate.dispatch(x, y, z, *data) }
    override fun close() = delegate.close()
}
