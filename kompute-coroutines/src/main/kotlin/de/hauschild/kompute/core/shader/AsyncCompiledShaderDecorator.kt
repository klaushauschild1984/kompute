package de.hauschild.kompute.core.shader

import de.hauschild.kompute.core.data.ShaderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decorator for [CompiledShader] that dispatches shader executions on a background thread.
 *
 * @param delegate the [CompiledShader] to decorate and delegate to
 */
internal class AsyncCompiledShaderDecorator(
    private val delegate: CompiledShader,
): AsyncCompiledShader{
    override suspend fun dispatch(
        x: Int,
        y: Int,
        z: Int,
        vararg data: ShaderData
    ) =
        withContext(Dispatchers.IO) { delegate.dispatch(x, y, z, *data) }
    override fun close() = delegate.close()
}
