package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.requireConfiguration

/**
 * Marker interface for all shader data bindings.
 *
 * Implemented by [IndexedBinding] (for buffer bindings) and [NamedBinding] (for uniform bindings).
 */
sealed interface Binding {
    /**
     * Validates this binding's configuration.
     *
     * @throws [de.hauschild.kompute.core.exception.KomputeConfigurationException] if the configuration is invalid
     */
    fun validate()
}

/**
 * @param label human-readable name of the validated property, used in the error message (e.g. `"index"`, `"name"`)
 * @param keySelector extracts the key to check for duplicates from each binding
 */
internal fun <T : Binding, K> List<T>.crossValidate(
    label: String,
    keySelector: (T) -> K,
) {
    val duplicates = groupBy(keySelector)
        .filter { it.value.size > 1 }
        .keys
    requireConfiguration(duplicates.isEmpty()) { "Duplicate $label: $duplicates" }
}
