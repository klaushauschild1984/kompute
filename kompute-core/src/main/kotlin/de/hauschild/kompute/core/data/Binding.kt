package de.hauschild.kompute.core.data

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
