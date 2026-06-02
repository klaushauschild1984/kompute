package de.hauschild.kompute.backend.kotlin

import de.hauschild.kompute.core.AbstractBackend
import de.hauschild.kompute.core.Backend.Type
import de.hauschild.kompute.core.ShaderBuilder

class KotlinBackend : AbstractBackend() {
    override fun type(): Type = Type.Kotlin

    override fun doInitialize() = Unit

    override fun shader(path: String): ShaderBuilder = TODO("Not yet implemented")

    fun shader(block: (BufferScope) -> Unit): ShaderBuilder = TODO("Not yet implemented")

    override fun close() = Unit
}
