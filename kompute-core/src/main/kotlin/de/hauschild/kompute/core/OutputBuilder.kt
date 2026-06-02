package de.hauschild.kompute.core

interface OutputBuilder {
    fun buffer(data: FloatArray): ShaderBuilder
}
