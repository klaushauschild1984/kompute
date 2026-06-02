package de.hauschild.kompute.core

import java.nio.file.Path

sealed interface ShaderSource {
    data class Code(
        val glsl: String,
    ) : ShaderSource

    data class File(
        val path: Path,
    ) : ShaderSource
}
