package de.hauschild.kompute.core.backend

import de.hauschild.kompute.core.InternalApi
import de.hauschild.kompute.core.shader.CompiledShader
import de.hauschild.kompute.core.shader.ShaderSource

/**
 * [Backend] test fixture registered via `META-INF/services` for [de.hauschild.kompute.core.Kompute] tests.
 *
 * Behavior of [doInitialize] is controlled per test via [doInitializeAction] and must be reset afterwards.
 */
@OptIn(InternalApi::class)
class TestBackend : AbstractBackend() {
    override fun type(): Type = Type.OpenGL

    override fun doInitialize() = doInitializeAction()

    override fun compileSource(source: ShaderSource): CompiledShader =
        throw UnsupportedOperationException("not needed for this test fixture")

    override fun close() = Unit

    companion object {
        var doInitializeAction: () -> Unit = { /* no-op */ }
    }
}
