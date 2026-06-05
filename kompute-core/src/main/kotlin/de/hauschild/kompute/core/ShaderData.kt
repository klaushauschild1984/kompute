package de.hauschild.kompute.core

import kotlin.reflect.KClass

/**
 * Represents data passed to or received from a compute shader.
 *
 * Defines the different types of shader data (storage buffers, etc.) that can be attached to
 * a shader computation. Each implementation validates its own configuration before execution.
 *
 * @see ShaderData.StorageBuffer
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
     * - A size-only buffer must have [asOutput] called to name the result
     *
     * Example:
     * ```
     * // Input buffer
     * StorageBuffer<FloatArray>(0).data(floatArrayOf(1f, 2f, 3f))
     *
     * // Output buffer
     * StorageBuffer<FloatArray>(1).size(128).asOutput("result")
     *
     * // Read-write buffer
     * StorageBuffer<FloatArray>(2).data(existing).asOutput("updated")
     * ```
     *
     * @param index the binding index in the shader — must be non-negative
     */
    class StorageBuffer<T: Any>(
        val index: Int,
        val type: KClass<T>,
    ) : ShaderData,
        OutputCapable {
        var data: T? = null
            private set
        var size: Int? = null
            private set
        override var outputName: String? = null
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
         * Marks this buffer as an output retrievable by the given name.
         *
         * Required when [size] is used.
         *
         * @param name a unique name to identify this output in [ShaderResult]
         * @return this [StorageBuffer] for chaining
         */
        fun asOutput(name: String): StorageBuffer<T> {
            this.outputName = name
            return this
        }

        /**
         * Validates the buffer configuration.
         *
         * @throws KomputeConfigurationException if the index is negative, neither [data] nor [size]
         * is provided, both are provided, or [size] is provided without an output name
         */
        override fun validate() {
            requireConfiguration(index >= 0) { "Index must be non-negative for StorageBuffer" }
            requireConfiguration(
                data != null || size != null,
            ) { "Either data or size must be provided for StorageBuffer" }
            if (data != null) {
                requireConfiguration(size == null) { "Size should not be combined together with data" }
            }
            if (size != null) {
                requireConfiguration(outputName != null) { "Output name must be provided for StorageBuffer with size" }
            }
        }

        companion object {
            fun crossValidate(storageBuffers: List<StorageBuffer<*>>) {
                val duplicates =
                    storageBuffers
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
     */
    interface OutputCapable {
        /** Optional output name */
        val outputName: String?

        /** Determines whether this [ShaderData] is used as output data or not. */
        fun isOutput(): Boolean = outputName != null
    }
}

inline fun <reified T : Any> StorageBuffer(index: Int) = ShaderData.StorageBuffer(index, T::class)
