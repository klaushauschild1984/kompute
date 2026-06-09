package de.hauschild.kompute.core.exception

/**
 * Thrown when the backend fails to initialize (e.g., GPU not available or already initialized).
 *
 * @param message
 * @param cause
 */
class KomputeBackendInitializationException(
    message: String,
    cause: Throwable? = null,
) : KomputeBackendException(message, cause)
