package de.hauschild.kompute.core

import java.io.InputStream
import java.nio.file.Path

/**
 * Represents the source code of a compute shader.
 *
 * Three variants are provided to cover different use cases:
 * - [Code] for inline GLSL strings, useful in tests or generated shaders
 * - [File] for shader files stored on disk
 * - [Stream] for classpath resources or other stream-based sources
 */
sealed interface ShaderSource {
    /**
     * Shader source loaded from an input stream, e.g. for classpath resources.
     *
     * The stream is read once and closed automatically by the backend.
     * The caller must not use the stream after passing it here.*
     * @property inputStream the input stream containing the complete compute shader source
     */
    data class Stream(
        val inputStream: InputStream,
    ) : ShaderSource

    /**
     * Inline GLSL source code.*
     * @property glsl the complete GLSL compute shader source code
     */
    data class Code(
        val glsl: String,
    ) : ShaderSource

    /**
     * Shader source loaded from a file on disk.
     *
     * The file must exist and be readable when the backend compiles the shader.*
     * @property path the path to the compute shader source file
     */
    data class File(
        val path: Path,
    ) : ShaderSource
}
