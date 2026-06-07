package de.hauschild.kompute.core.data

/**
 * Describes the capability of a [ShaderData] to act as output data.
 * Use this object itself as key to retrieve result data from [ShaderResult].
 *
 * @param T the data type returned after execution — matches the type parameter of the buffer
 */
interface OutputCapable<T : Any> {
    /**
     * Determines whether this [ShaderData] is used as output data or not.
     */
    val isOutput: Boolean
}
