package de.hauschild.kompute.core.exception

/**
 * Base class for all Kompute runtime exceptions.
 *
 * @param message
 * @param cause
 */
abstract class KomputeException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
