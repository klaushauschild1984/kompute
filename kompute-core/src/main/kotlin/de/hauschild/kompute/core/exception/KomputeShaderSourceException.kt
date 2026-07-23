package de.hauschild.kompute.core.exception

/**
 * Thrown when a [de.hauschild.kompute.core.shader.ShaderSource] cannot be resolved to its
 * underlying content, e.g. because a file or stream could not be read.
 *
 * @param message
 * @param cause
 */
class KomputeShaderSourceException(
    message: String,
    cause: Throwable? = null,
) : KomputeException(message, cause)
