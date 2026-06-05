package de.hauschild.kompute.core

import de.hauschild.kompute.core.ShaderData.StorageBuffer
import kotlin.reflect.KClass

/**
 * Represents data passed to or received from a compute shader.
 *
 * Defines the different types of shader data (storage buffers, etc.) that can be attached to
 * a shader computation. Each implementation validates its own configuration before execution.
 *
 * @see StorageBuffer
 */
sealed interface ShaderData {
    /**
     * Validates this shader data configuration.
     *
     * @throws KomputeConfigurationException if the configuration is invalid
     */
    fun validate()

    /**
     * A storage buffer that can be read and/or written by the compute shader.
     *
     * Storage buffers are the primary data exchange mechanism between host and GPU.
     * Each buffer is bound to a binding index declared in the shader source.
     *
     * Configuration rules:
     * - Exactly one of [data] or [size] must be provided
     * - If [data] is provided, the buffer is initialized with the given input data
     * - If [size] is provided, an empty buffer of that size is allocated (output only)
     * - A size-only buffer must have [asOutput] called to mark it as output
     *
     * Example:
     * ```
     * // Input buffer
     * StorageBuffer<FloatArray>(0).data(floatArrayOf(1f, 2f, 3f))
     *
     * // Output buffer
     * StorageBuffer<FloatArray>(1).size(128).asOutput()
     *
     * // Read-write buffer
     * StorageBuffer<FloatArray>(2).data(existing).asOutput()
     * ```
     *
     * @param index the binding index in the shader — must be non-negative
     */
    class StorageBuffer<T : Any>(
        val index: Int,
        val type: KClass<T>,
    ) : ShaderData,
        OutputCapable<T> {
        var data: T? = null
            private set
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
         * @throws KomputeConfigurationException if the index is negative, neither [data] nor [size]
         * is provided, both are provided, or [size] is provided without calling [asOutput]
         */
        override fun validate() {
            requireConfiguration(type in SUPPORTED_TYPES) {
                "Unsupported StorageBuffer type: ${type.simpleName}"
            }
            requireConfiguration(index >= 0) {
                "Index must be non-negative for StorageBuffer"
            }
            requireConfiguration(
                data != null || size != null,
            ) { "Either data or size must be provided for StorageBuffer" }
            if (data != null) {
                requireConfiguration(size == null) {
                    "Size should not be combined together with data"
                }
            }
            if (size != null) {
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

            fun crossValidate(storageBuffers: List<StorageBuffer<*>>) {
                val duplicates =
                    storageBuffers
                        .map { it.index }
                        .groupBy { it }
                        .filter { (_, occurrences) -> occurrences.size > 1 }
                        .keys
                requireConfiguration(duplicates.isEmpty()) { "There are duplicated indices: $duplicates" }
            }

            /**
             * Convinience methode to create a [StorageBuffer] from Java using the [Class] type.
             */
            @JvmStatic
            fun <T : Any> newStorageBuffer(
                index: Int,
                type: Class<T>,
            ) = StorageBuffer(index, type.kotlin)
        }
    }

    /**
     * Describes the capability of a [ShaderData] to act as output data.
     * Use this object itself as key to retrieve result data from [ShaderResult].
     */
    interface OutputCapable<T : Any> {
        /** Determines whether this [ShaderData] is used as output data or not. */
        val isOutput: Boolean
    }
}

@Suppress("FunctionNaming")
inline fun <reified T : Any> StorageBuffer(index: Int) = StorageBuffer(index, T::class)
