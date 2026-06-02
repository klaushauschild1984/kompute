package de.hauschild.kompute.core

import java.io.InputStream
import java.nio.file.Path

/**
 * Represents the source of a compute shader.
 */
sealed interface ShaderSource {
    /**
     * Represents the shader as plain source code.
     * @param glsl the GLSL source code of the compute shader
     */
    data class Code(
        val glsl: String,
    ) : ShaderSource

    /**
     * Represents the shader as a file on disk.
     * @param path the path to the compute shader file
     */
    data class File(
        val path: Path,
    ) : ShaderSource

    /**
     * Represents the shader as an input stream, e.g. for classpath resources.
     * The stream will be read once and closed afterwards.
     * @param inputStream the input stream containing the compute shader source
     */
    data class Stream(
        val inputStream: InputStream,
    ) : ShaderSource
}
