package de.hauschild.kompute.core

interface InputBuilder {
    fun buffer(data: FloatArray): ShaderBuilder
}
