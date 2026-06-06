package de.hauschild.kompute.core.exception

/**
 * @param message
 * @param cause
 */
abstract class KomputeBackendException(
    message: String,
    cause: Throwable? = null,
) : KomputeException(message, cause)
