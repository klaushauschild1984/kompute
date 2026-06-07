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
 * @param keySelector
 * @param label
 */
internal fun <T : Binding, K> List<T>.crossValidate(
    keySelector: (T) -> K,
    label: String,
) {
    groupBy(keySelector)
        .filter { it.value.size > 1 }
        .keys
        .forEach { requireConfiguration(false) { "Duplicate $label: $it" } }
}
