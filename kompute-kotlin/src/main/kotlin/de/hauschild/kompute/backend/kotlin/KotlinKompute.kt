package de.hauschild.kompute.backend.kotlin

import de.hauschild.kompute.core.Kompute
import de.hauschild.kompute.core.ShaderBuilder

class KotlinKompute : Kompute {
    override fun shader(path: String): ShaderBuilder = TODO("Not yet implemented")

    fun shader(block: (BufferScope) -> Unit): ShaderBuilder = TODO("Not yet implemented")

    override fun close() = Unit
}
