package de.hauschild.kompute.core

interface ShaderResult {
    fun <T> getOutput(name: String): T
}
