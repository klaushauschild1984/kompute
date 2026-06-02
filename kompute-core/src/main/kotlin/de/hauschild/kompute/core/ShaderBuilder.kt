package de.hauschild.kompute.core

interface ShaderBuilder {
    fun inputBuffer(
        name: String,
        data: FloatArray,
    ): ShaderBuilder

    fun outputBuffer(
        name: String,
        data: FloatArray,
    ): ShaderBuilder

    fun dispatch(
        x: Int,
        y: Int,
        z: Int,
    ): ShaderBuilder

    fun execute(): ShaderResult
}
