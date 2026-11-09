package de.hauschild.kompute.core.exception

/**
 * Base class for all Kompute runtime exceptions.
 *
 * @param message
 * @param cause
 */
@Suppress("AbstractClassCanBeConcreteClass") // abstract on purpose: only the concrete subtypes are thrown
abstract class KomputeException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
