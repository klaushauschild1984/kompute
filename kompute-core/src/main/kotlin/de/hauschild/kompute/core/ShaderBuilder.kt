package de.hauschild.kompute.core

interface ShaderBuilder {
    fun input(name: String): InputBuilder

    fun output(name: String): OutputBuilder

    fun dispatch(
        x: Int,
        y: Int = 1,
        z: Int = 1,
    ): DispatchBuilder
}
