package de.hauschild.kompute.core

interface Backend : AutoCloseable {
    fun type(): Type

    fun initialize()

    fun shader(path: String): ShaderBuilder
}
