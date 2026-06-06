package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.requireConfiguration

/**
 * Describes a binding identified by name for shader data.
 *
 * Implemented by shader data types that are bound by name rather than index, such as scalar uniforms.
 */
interface NamedBinding : Binding {
    /**
     * The binding name in the shader — must not be blank.
     */
    val name: String

    override fun validate() {
        requireConfiguration(name.isNotBlank()) {
            "Name must not be blank"
        }
    }

    companion object {
        /**
         * Validates that no two named bindings share the same binding index.
         *
         * @param namedBindings the list of named bindings to cross-validate
         * @throws [KomputeConfigurationException] if duplicate indices are found
         */
        fun crossValidate(namedBindings: List<NamedBinding>) {
            val duplicates =
                namedBindings
                    .map { it.name }
                    .groupBy { it }
                    .filter { (_, occurrences) -> occurrences.size > 1 }
                    .keys
            requireConfiguration(duplicates.isEmpty()) { "There are duplicated names: $duplicates" }
        }
    }
}
