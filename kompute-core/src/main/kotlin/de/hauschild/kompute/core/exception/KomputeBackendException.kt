package de.hauschild.kompute.core.exception

/**
 * Thrown when a backend-related error occurs during initialization or computation.
 *
 * @param message
 * @param cause
 */
abstract class KomputeBackendException(
    message: String,
    cause: Throwable? = null,
) : KomputeException(message, cause)
