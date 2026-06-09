/**
 * Bundles all require-like functions.
 */

package de.hauschild.kompute.core.exception

/**
 * @param value the condition that must be `true`
 * @param lazyMessage produces the error message if [value] is `false`
 * @throws KomputeConfigurationException if [value] is `false`
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
 * @param value the condition that must be `true`
 * @param lazyMessage produces the error message if [value] is `false`
 * @throws KomputeBackendInitializationException if [value] is `false`
 */
fun requireBackendInitialization(
    value: Boolean,
    lazyMessage: () -> String,
) {
    if (!value) {
        throw KomputeBackendInitializationException(lazyMessage())
    }
}
