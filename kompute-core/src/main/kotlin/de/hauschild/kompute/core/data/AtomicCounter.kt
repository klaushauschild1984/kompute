package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.requireConfiguration

/**
 * An atomic counter that provides a GPU-side unsigned integer counter accessible in compute shaders.
 *
 * Atomic counters are declared in GLSL with `layout(binding = N) uniform atomic_uint` and always
 * operate in read-write mode: an initial value is uploaded before dispatch and the result is read
 * back afterwards.
 *
 * The GLSL type `atomic_uint` is an unsigned 32-bit integer — negative initial values are rejected
 * during validation.
 *
 * ```kotlin
 * AtomicCounter(0)                 // starts at 0
 * AtomicCounter(1).start(100)      // starts at 100
 * ```
 *
 * @property index the binding index in the shader — must be non-negative
 */
class AtomicCounter(
    override val index: Int
) : ShaderData,
IndexedBinding,
OutputCapable<Int> {
    override val isOutput: Boolean = true

    /**
     * The initial counter-value uploaded to the GPU before dispatch. Defaults to 0.
     */
    var start: Int = 0
        private set

    /**
     * Sets the initial counter-value.
     *
     * @param start the value to initialize the counter with — must be non-negative
     * @return this [AtomicCounter] for chaining
     */
    fun start(start: Int): AtomicCounter {
        this.start = start
        return this
    }

    override fun validate() {
        super.validate()
        requireConfiguration(start >= 0) {
            "Counter start must be non-negative"
        }
    }

    override fun toString()= "AtomicCounter($index)${if (start != 0) ".start($start)" else ""}"
}
