package de.hauschild.kompute.opengl

/**
 * Represents a resource that can be bound to a shader program.
 */
interface Bindable : AutoCloseable {
    /**
     * Binds this resource to the currently active shader program.
     */
    fun bind()
}
