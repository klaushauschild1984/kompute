package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.requireConfiguration

/**
 * Describes a binding index for shader data.
 */
interface IndexedBinding {
    /**
     * The binding index in the shader.
     */
    val index: Int

    /**
     * Validates the binding index.
     *
     * @throws [KomputeConfigurationException] if the index is negative
     */
    fun validate() {
        requireConfiguration(index >= 0) {
            "Index must be non-negative"
        }
    }

    /**
     * Validates that no two index bindings share the same binding index.
     *
     * @param indexBindings the list of index bindings to cross-validate
     * @throws [KomputeConfigurationException] if duplicate indices are found
     */
    companion object {
        /**
         * @param indexedBindings
         */
        fun crossValidate(indexedBindings: List<IndexedBinding>) {
            val duplicates =
                indexedBindings
                    .map { it.index }
                    .groupBy { it }
                    .filter { (_, occurrences) -> occurrences.size > 1 }
                    .keys
            requireConfiguration(duplicates.isEmpty()) { "There are duplicated indices: $duplicates" }
        }
    }
}
