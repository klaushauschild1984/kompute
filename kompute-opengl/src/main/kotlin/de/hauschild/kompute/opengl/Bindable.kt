package de.hauschild.kompute.opengl

interface Bindable : AutoCloseable {
    /**
     * Binds this resource to the currently active shader program.
     */
    fun bind()
}
