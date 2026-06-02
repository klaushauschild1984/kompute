package de.hauschild.kompute.core

/**
 * Triggers the execution of the configured compute shader.
 */
class ShaderResult(
    private val outputs: Map<String, FloatArray>,
) {
    /**
     * Retrieve the output parameter with the given name.
     * @param name the name of the output parameter to retrieve
     */
    fun output(name: String): FloatArray = outputs[name] ?: error("No output named '$name'")
}
