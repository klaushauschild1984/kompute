/**
 * Extension functions for [CompiledShader] to enable asynchronous dispatch via Kotlin Coroutines.
 */

package de.hauschild.kompute.core.shader

/**
 * Wraps this [CompiledShader] in an [AsyncCompiledShader] that suspends on [kotlinx.coroutines.Dispatchers.IO].
 *
 * Note: concurrent dispatches on the [de.hauschild.kompute.core.backend.Type.OpenGL] backend are not supported.
 *
 * @return an [AsyncCompiledShader] backed by this shader
 */
fun CompiledShader.async(): AsyncCompiledShader = AsyncCompiledShaderDecorator(this)
