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
 * - [IntArray] → `int` / `ivec*` / `uint` / `uvec*`
 * - [LongArray] → `int64_t` / `uint64_t`
 * - [FloatArray] → `float` / `vec*` / `mat*`
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
     * Returns the resolved [Mode] of this buffer based on the current configuration.
     *
     * Must only be called after [validate].
     *
     * @return [Mode] the [StorageBuffer] operates in
     */
    fun mode(): Mode<T> = when {
        isOutput && data != null -> Mode.ReadWrite(data!!)
        isOutput -> Mode.Output(size!!)
        else -> Mode.Input(data!!)
    }

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
     * Valid modes after validation:
     * - [Mode.Input]: [data] set, no [size], [asOutput] not called
     * - [Mode.Output]: [size] set, no [data], [asOutput] called
     * - [Mode.ReadWrite]: [data] set, no [size], [asOutput] called
     *
     * @throws KomputeConfigurationException if the index is negative,
     * the type is unsupported,
     * neither [data] nor [size] is provided, both are provided, or [size] is used without [asOutput]
     */
    override fun validate() {
        super.validate()
        requireConfiguration(type in SUPPORTED_TYPES) {
            "Unsupported StorageBuffer type: ${type.simpleName}"
        }
        requireConfiguration(data != null || size != null) {
            "Either data or size must be provided"
        }
        requireConfiguration(data == null || size == null) {
            "Size must not be combined with data"
        }
        requireConfiguration(size == null || isOutput) {
            "Sized StorageBuffer must be marked as output"
        }
    }

    override fun toString(): String = buildString {
        append("StorageBuffer<${type.simpleName}>($index)")
        data?.let { append(".data([${it.elementCount()} elements])") }
        size?.let { append(".size($it)") }
        if (isOutput) {
            append(".asOutput()")
        }
    }

    private fun Any.elementCount(): Int = when (this) {
        is IntArray -> size
        is LongArray -> size
        is FloatArray -> size
        is DoubleArray -> size
        is ByteArray -> size
        else -> -1
    }

    /**
     * The different modes a [StorageBuffer] can be in.
     *
     * @param T the buffer data type (e.g. [FloatArray], [IntArray])
     */
    sealed class Mode<out T> {
        /**
         * Input data to upload to the GPU.
         *
         * @param T the buffer data type
         * @property data the data to upload
         */
        data class Input<T>(val data: T) : Mode<T>()

        /**
         * Output buffer that is allocated on the GPU.
         *
         * @param T the buffer data type
         * @property size the number of elements to allocate
         */
        data class Output<T>(val size: Int) : Mode<T>()

        /**
         * Read-write buffer that is allocated on the GPU.
         *
         * @param T the buffer data type
         * @property data the initial data to upload
         */
        data class ReadWrite<T>(val data: T) : Mode<T>()
    }

    companion object {
        private val SUPPORTED_TYPES =
            setOf(
                IntArray::class,
                LongArray::class,
                FloatArray::class,
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
