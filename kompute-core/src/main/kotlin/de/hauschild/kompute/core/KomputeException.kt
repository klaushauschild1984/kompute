/**
 * Exception hierarchy for Kompute errors.
 *
 * Contains base exceptions and specific subtypes for configuration failures, backend
 * initialization errors, and dispatch errors, as well as utility functions for throwing them.
 */

package de.hauschild.kompute.core

/**
 * @param message
 * @param cause
 */
abstract class KomputeException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * @param message
 */
class KomputeConfigurationException(
    message: String,
) : KomputeException(message)

/**
 * @param message
 * @param cause
 */
abstract class KomputeBackendException(
    message: String,
    cause: Throwable? = null,
) : KomputeException(message, cause)

/**
 * @param message
 * @param cause
 */
class KomputeBackendInitializationException(
    message: String,
    cause: Throwable? = null,
) : KomputeBackendException(message, cause)

/**
 * @param message
 * @param cause
 */
class KomputeBackendDispatchException(
    message: String,
    cause: Throwable? = null,
) : KomputeBackendException(message, cause)

/**
 * @param value
 * @param lazyMessage
 * @throws KomputeConfigurationException
 */
fun requireConfiguration(
    value: Boolean,
    lazyMessage: () -> String,
) {
    if (!value) {
        throw KomputeConfigurationException(lazyMessage())
    }
}

/**
 * @param value
 * @param lazyMessage
 * @throws KomputeBackendInitializationException
 */
fun requireBackendInitialization(
    value: Boolean,
    lazyMessage: () -> String,
) {
    if (!value) {
        throw KomputeBackendInitializationException(lazyMessage())
    }
}
