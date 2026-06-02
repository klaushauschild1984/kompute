package de.hauschild.kompute.core

class ExecutionContext(
    val source: ShaderSource,
) {
    val inputs = mutableMapOf<String, FloatArray>()
    val outputs = mutableMapOf<String, FloatArray>()
    var x = 1
    var y = 1
    var z = 1
}
