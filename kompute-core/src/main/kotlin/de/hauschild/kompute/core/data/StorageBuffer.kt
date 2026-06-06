package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.requireConfiguration
import kotlin.reflect.KClass

/**
 * A storage buffer that exchanges data between CPU and compute shader via a binding index.
 *
 * Storage buffers are the primary data exchange mechanism between host and GPU. They can be
 * used as input (initialized with [data]), output (sized with [size] and marked via [asOutput]),
 * or read-write (both [data] and [asOutput] combined). Each buffer is bound to a binding index
 * declared in the shader source with `layout(std430, binding = N)`.
 *
 * Supported types and their GLSL equivalents:
 * - [FloatArray] → `float` / `vec*` / `mat*`
 * - [IntArray] → `int` / `ivec*` / `uint` / `uvec*`
 * - [DoubleArray] → `double` / `dvec*`
 * - [ByteArray] → struct (manual layout)
 *
 * ```kotlin
 * StorageBuffer<FloatArray>(0).data(floatArrayOf(1f, 2f, 3f))   // input
 * StorageBuffer<FloatArray>(1).size(128).asOutput()              // output
 * StorageBuffer<FloatArray>(2).data(existing).asOutput()         // read-write
 * ```
 *
 * @param T the GPU data type — must be one of the supported types listed above
 * @property index the binding index in the shader — must be non-negative
 * @property type the [KClass] of [T], used to determine GPU buffer layout and data transfer
 */
class StorageBuffer<T : Any>(
    override val index: Int,
    val type: KClass<T>,
) : ShaderData,
IndexedBinding,
OutputCapable<T> {
    /**
     * Input data to upload to the GPU, or null if not set.
     */
    var data: T? = null
        private set

    /**
     * Number of elements to allocate for an output-only buffer, or null if not set.
     */
    var size: Int? = null
        private set
    override var isOutput: Boolean = false
        private set

    /**
     * Sets the input data for this buffer.
     *
     * Cannot be combined with [size].
     *
     * @param data the data to upload to the GPU
     * @return this [StorageBuffer] for chaining
     */
    fun data(data: T): StorageBuffer<T> {
        this.data = data
        return this
    }

    /**
     * Sets the output size for this buffer.
     *
     * An empty buffer of this size is allocated on the GPU. The shader writes results here.
     * Must be combined with [asOutput]. Cannot be combined with [data].
     *
     * @param size the number of elements to allocate
     * @return this [StorageBuffer] for chaining
     */
    fun size(size: Int): StorageBuffer<T> {
        this.size = size
        return this
    }

    /**
     * Marks this buffer as an output.
     *
     * Required when [size] is used.
     *
     * @return this [StorageBuffer] for chaining
     */
    fun asOutput(): StorageBuffer<T> {
        this.isOutput = true
        return this
    }

    /**
     * Validates the buffer configuration.
     *
     * @throws [KomputeConfigurationException] if the index is negative, neither [data] nor [size]
     * is provided, both are provided, or [size] is provided without calling [asOutput]
     */
    override fun validate() {
        super.validate()
        requireConfiguration(type in SUPPORTED_TYPES) {
            "Unsupported StorageBuffer type: ${type.simpleName}"
        }
        requireConfiguration(
            data != null || size != null,
        ) { "Either data or size must be provided" }
        data?.let {
            requireConfiguration(size == null) {
                "Size should not be combined together with data"
            }
        }
        size?.let {
            requireConfiguration(isOutput) {
                "Sized StorageBuffer must be marked as output"
            }
        }
    }

    override fun toString(): String = "StorageBuffer<${type.simpleName}>(index=$index)"

    companion object {
        private val SUPPORTED_TYPES =
            setOf(
                FloatArray::class,
                IntArray::class,
                DoubleArray::class,
                ByteArray::class,
            )

        /**
         * Creates a [StorageBuffer] with a reified type parameter for idiomatic Kotlin usage.
         *
         * @param index the binding index in the shader — must be non-negative
         * @return a new [StorageBuffer] with the inferred type [T]
         */
        inline operator fun <reified T : Any> invoke(index: Int) = StorageBuffer(index, T::class)

        /**
         * Convenience method to create a [StorageBuffer] from Java using the [Class] type.
         *
         * @param index the binding index in the shader
         * @param type the Java [Class] of the buffer data type
         * @return a new [StorageBuffer] with the given index and type
         */
        @JvmStatic
        fun <T : Any> newStorageBuffer(
            index: Int,
            type: Class<T>,
        ) = StorageBuffer(index, type.kotlin)
    }
}
