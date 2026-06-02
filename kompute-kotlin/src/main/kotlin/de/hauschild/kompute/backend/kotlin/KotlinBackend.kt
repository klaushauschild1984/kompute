package de.hauschild.kompute.backend.kotlin

import de.hauschild.kompute.core.AbstractBackend
import de.hauschild.kompute.core.ShaderBuilder
import de.hauschild.kompute.core.ShaderSource
import de.hauschild.kompute.core.Type

class KotlinBackend : AbstractBackend() {
    override fun type(): Type = Type.Kotlin

    override fun doInitialize() = Unit

    override fun shader(source: ShaderSource): ShaderBuilder = TODO("Not yet implemented")

    fun shader(block: (BufferScope) -> Unit): ShaderBuilder = TODO("Not yet implemented")

    override fun close() = Unit
}
