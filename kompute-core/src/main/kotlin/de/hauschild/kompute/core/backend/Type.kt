package de.hauschild.kompute.core.backend

/**
 * Enumeration of available GPU compute backend types.
 *
 * Used by [de.hauschild.kompute.core.Kompute] to identify and load specific backend implementations.
 */
sealed interface Type {
    /**
     * OpenGL compute backend.
     */
    data object OpenGL : Type
}
