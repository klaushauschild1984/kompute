package de.hauschild.kompute.core

interface Kompute : AutoCloseable {
    fun shader(path: String): ShaderBuilder
}
