package de.hauschild.kompute.core

/**
 * Enumeration of available GPU compute backend types.
 *
 * Used by [Kompute] to identify and load specific backend implementations.
 */
sealed interface Type {
    /** OpenGL compute backend using GLSL compute shaders. */
    data object OpenGL : Type
}
