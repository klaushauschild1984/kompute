package de.hauschild.kompute.core.shader

import de.hauschild.kompute.core.data.ShaderData

/**
 * Mock implementation of [CompiledShader] for testing.
 */
internal class CompiledShaderMock : CompiledShader {
    /**
     *  Whether [dispatch] was called.
     */
    var dispatched: Boolean = false

    /**
     * Whether [close] was called.
     */
    var closed: Boolean = false

    /**
     * Registers the dispatch call.
     *
     * @param x ignored
     * @param y ignored
     * @param z ignored
     * @param data ignored
     * @return empty result
     */
    override fun dispatch(
        x: Int,
        y: Int,
        z: Int,
        vararg data: ShaderData
    ): ShaderResult {
        dispatched = true
        return ShaderResult(emptyMap())
    }

    /**
     * Registers the close call.
     */
    override fun close() {
        closed = true
    }
}
