/**
 * Bundles all require-like functions.
 */

package de.hauschild.kompute.core.exception

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
