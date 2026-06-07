package de.hauschild.kompute.core.exception

/**
 * @param message
 * @param cause
 */
class KomputeBackendDispatchException(
    message: String,
    cause: Throwable? = null,
) : KomputeBackendException(message, cause)
