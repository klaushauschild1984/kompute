package de.hauschild.kompute.core.exception

/**
 * @param message
 * @param cause
 */
abstract class KomputeException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
