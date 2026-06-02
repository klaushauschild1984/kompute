package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.AbstractBackend
import de.hauschild.kompute.core.ExecutionContext
import de.hauschild.kompute.core.InternalApi
import de.hauschild.kompute.core.ShaderResult
import de.hauschild.kompute.core.Type

class OpenGLBackend : AbstractBackend() {
    @InternalApi
    override fun type(): Type = Type.OpenGL

    override fun doInitialize() = Unit

    override fun execute(context: ExecutionContext): ShaderResult {
        TODO("Not yet implemented")
    }

    override fun close() = Unit
}
