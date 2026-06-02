package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.AbstractBackend
import de.hauschild.kompute.core.Backend.Type
import de.hauschild.kompute.core.ShaderBuilder

class OpenGLBackend : AbstractBackend() {
    override fun type(): Type = Type.OpenGL

    override fun doInitialize() = Unit

    override fun shader(path: String): ShaderBuilder = TODO("Not yet implemented")

    override fun close() = Unit
}
