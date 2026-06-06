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
     * @param T the GPU data type — must be [FloatArray], [IntArray], [DoubleArray], or [ByteArray]
     * @property index the binding index in the shader — must be non-negative
     * @property type the [KClass] of [T], used to determine GPU buffer layout and data transfer
     */
    class StorageBuffer<T : Any>(
        override val index: Int,
        val type: KClass<T>,
    ) : ShaderData,
    IndexBinding,
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
         * @throws KomputeConfigurationException if the index is negative, neither [data] nor [size]
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

    /**
     * Describes a binding index for shader data.
     */
    interface IndexBinding {
        /**
         * The binding index in the shader.
         */
        val index: Int

        /**
         * Validates the binding index.
         *
         * @throws KomputeConfigurationException if the index is negative
         */
        fun validate() {
            requireConfiguration(index >= 0) {
                "Index must be non-negative"
            }
        }

        /**
         * Validates that no two index bindings share the same binding index.
         *
         * @param indexBindings the list of index bindings to cross-validate
         * @throws KomputeConfigurationException if duplicate indices are found
         */
        companion object {
            /**
             * @param indexBindings
             */
            fun crossValidate(indexBindings: List<IndexBinding>) {
                val duplicates =
                    indexBindings
                        .map { it.index }
                        .groupBy { it }
                        .filter { (_, occurrences) -> occurrences.size > 1 }
                        .keys
                requireConfiguration(duplicates.isEmpty()) { "There are duplicated indices: $duplicates" }
            }
        }
    }

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

    /**
     * A uniform buffer that passes read-only configuration data from the CPU to the compute shader.
     *
     * UBOs are bound to a binding index declared in the shader source and follow the std140 memory
     * layout — `vec3` fields are aligned to 16 bytes and require manual padding in the data array.
     * Unlike [StorageBuffer], a uniform buffer cannot be written by the shader.
     *
     * @property index the binding index in the shader — must be non-negative
     */
    class UniformBuffer(override val index: Int) : ShaderData, IndexBinding {
        /**
         * Input data to upload to the GPU, or null if not set.
         */
        var data: ByteArray? = null
            private set

        /**
         * Sets the input data for this buffer.
         *
         * @param data the data to upload to the GPU
         * @return this [UniformBuffer] for chaining
         */
        fun data(data: ByteArray): UniformBuffer {
            this.data = data
            return this
        }

        /**
         * Validates the buffer configuration.
         *
         * @throws KomputeConfigurationException if the index is negative, [data] not provided
         */
        override fun validate() {
            super.validate()
            requireConfiguration(data != null) {
                "Data must be provided"
            }
        }

        override fun toString(): String = "UniformBuffer(index=$index)"
    }
}

/**
 * Creates a [ShaderData.StorageBuffer] with a reified type parameter.
 *
 * @param index the binding index in the shader — must be non-negative
 * @return a new [ShaderData.StorageBuffer] with the inferred type [T]
 */
@Suppress("FunctionNaming")
inline fun <reified T : Any> StorageBuffer(index: Int) = StorageBuffer(index, T::class)
