package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.requireConfiguration

/**
 * Describes a binding index for shader data.
 */
interface IndexedBinding : Binding {
    /**
     * The binding index in the shader.
     */
    val index: Int

    /**
     * Validates the binding index.
     *
     * @throws KomputeConfigurationException if the index is negative
     */
    override fun validate() {
        requireConfiguration(index >= 0) {
            "Index must be non-negative"
        }
    }

    companion object {
        /**
         * Validates that no two indexed bindings share the same binding index.
         *
         * @param indexedBindings the list of indexed bindings to cross-validate
         * @throws KomputeConfigurationException if duplicate indices are found
         */
        fun crossValidate(indexedBindings: List<IndexedBinding>) {
            indexedBindings.crossValidate("index") { it.index }
        }
    }
}
