package de.hauschild.kompute.core.shader

import de.hauschild.kompute.core.exception.KomputeShaderSourceException
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path

/**
 * Represents the source of a compute shader.
 *
 * Two formats are supported:
 * - [Glsl] for GLSL source text, compiled by the backend at runtime.
 * - [Spirv] for precompiled SPIR-V bytecode.
 *
 * Each format provides the same three origin variants for how the raw data is obtained: an
 * inline value (useful in tests or generated shaders), [File] for shader files stored on disk,
 * and [Stream] for classpath resources or other stream-based sources.
 */
sealed interface ShaderSource {
    /**
     * GLSL source code, compiled by the backend at runtime.
     */
    sealed interface Glsl : ShaderSource {
        /**
         * Resolves this source to the complete GLSL compute shader source code, regardless of
         * origin.
         *
         * @return the complete GLSL compute shader source code.
         * @throws KomputeShaderSourceException if the file or stream cannot be read.
         */
        fun resolve(): String = when (this) {
            is Code -> glsl
            is File -> readSource("GLSL file '$path'") { path.toFile().readText() }
            is Stream -> readSource("GLSL stream") { inputStream.use { it.reader().readText() } }
        }

        /**
         * Inline GLSL source code.
         *
         * @property glsl the complete GLSL compute shader source code.
         */
        data class Code(
            val glsl: String,
        ) : Glsl

        /**
         * GLSL source loaded from an input stream, e.g. for classpath resources.
         *
         * The stream is read once and closed automatically by the backend.
         * The caller must not use the stream after passing it here.
         *
         * @property inputStream the input stream containing the complete GLSL compute shader source.
         */
        data class Stream(
            val inputStream: InputStream,
        ) : Glsl

        /**
         * GLSL source loaded from a file on disk.
         *
         * The file must exist and be readable when the backend compiles the shader.
         *
         * @property path the path to the GLSL compute shader source file.
         */
        data class File(
            val path: Path,
        ) : Glsl
    }

    /**
     * Precompiled SPIR-V bytecode.
     */
    sealed interface Spirv : ShaderSource {
        /**
         * Resolves this source to the complete SPIR-V bytecode, regardless of origin.
         *
         * @return the complete SPIR-V bytecode.
         * @throws KomputeShaderSourceException if the file or stream cannot be read.
         */
        fun resolve(): ByteArray = when (this) {
            is Bytecode -> bytecode
            is File -> readSource("SPIR-V file '$path'") { path.toFile().readBytes() }
            is Stream -> readSource("SPIR-V stream") { inputStream.use { it.readBytes() } }
        }

        /**
         * Inline SPIR-V bytecode.
         *
         * @property bytecode the complete SPIR-V bytecode.
         */
        data class Bytecode(
            val bytecode: ByteArray,
        ) : Spirv

        /**
         * SPIR-V bytecode loaded from an input stream, e.g. for classpath resources.
         *
         * The stream is read once and closed automatically by the backend.
         * The caller must not use the stream after passing it here.
         *
         * @property inputStream the input stream containing the complete SPIR-V bytecode.
         */
        data class Stream(
            val inputStream: InputStream,
        ) : Spirv

        /**
         * SPIR-V bytecode loaded from a file on disk.
         *
         * The file must exist and be readable when the backend compiles the shader.
         *
         * @property path the path to the SPIR-V bytecode file.
         */
        data class File(
            val path: Path,
        ) : Spirv
    }
}

private inline fun <T> readSource(description: String, block: () -> T): T =
    try {
        block()
    } catch (exception: IOException) {
        throw KomputeShaderSourceException("Failed to read $description", exception)
    }
