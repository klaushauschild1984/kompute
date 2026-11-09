/**
 * Bundles all require-like functions.
 */

package de.hauschild.kompute.core.exception

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * @param value the condition that must be `true`
 * @param lazyMessage produces the error message if [value] is `false`
 * @throws KomputeConfigurationException if [value] is `false`
 */
@OptIn(ExperimentalContracts::class)
fun requireConfiguration(
    value: Boolean,
    lazyMessage: () -> String,
) {
    contract { returns() implies value }
    if (!value) {
        throw KomputeConfigurationException(lazyMessage())
    }
}

/**
 * @param value the condition that must be `true`
 * @param lazyMessage produces the error message if [value] is `false`
 * @throws KomputeBackendInitializationException if [value] is `false`
 */
@OptIn(ExperimentalContracts::class)
fun requireBackendInitialization(
    value: Boolean,
    lazyMessage: () -> String,
) {
    contract { returns() implies value }
    if (!value) {
        throw KomputeBackendInitializationException(lazyMessage())
    }
}
