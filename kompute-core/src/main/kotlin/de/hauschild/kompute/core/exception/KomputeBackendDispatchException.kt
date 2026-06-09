package de.hauschild.kompute.core.exception

/**
 * Thrown when a compute dispatch fails at the GPU level.
 *
 * @param message
 * @param cause
 */
class KomputeBackendDispatchException(
    message: String,
    cause: Throwable? = null,
) : KomputeBackendException(message, cause)
