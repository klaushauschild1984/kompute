package de.hauschild.kompute.core.exception

/**
 * Thrown when a backend-related error occurs during initialization or computation.
 *
 * @param message
 * @param cause
 */
@Suppress("AbstractClassCanBeConcreteClass") // abstract on purpose: only the concrete subtypes are thrown
abstract class KomputeBackendException(
    message: String,
    cause: Throwable? = null,
) : KomputeException(message, cause)
