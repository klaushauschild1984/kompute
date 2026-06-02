package de.hauschild.kompute.core

class ExecutionContext(
    val source: ShaderSource,
) {
    val inputs = mutableMapOf<Int, FloatArray>()
    val outputs = mutableMapOf<Pair<Int, String>, FloatArray>()
    var x = 1
    var y = 1
    var z = 1
}
