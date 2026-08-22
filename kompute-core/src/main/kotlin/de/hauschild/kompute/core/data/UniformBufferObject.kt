package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.requireConfiguration

/**
 * A uniform buffer object that passes read-only configuration data from the CPU to the compute shader.
 *
 * UBOs are ideal for shader parameters like viewport dimensions, zoom levels, or transformation
 * matrices. Unlike [StorageBuffer], the shader cannot write to a uniform buffer object. Each buffer is
 * bound to a binding index declared in the shader source with `layout(std140, binding = N)` or
 * `layout(std430, binding = N)`.
 *
 * The bytes passed to [data] must already match the memory layout declared by the shader —
 * this class performs no layout validation or conversion. Under std140, for example, `vec3`
 * fields are aligned to 16 bytes and require manual padding in the [ByteArray], as shown below.
 *
 * For straightforward serialization, prefer annotating a data class with `@GpuStruct`/`@GpuField`/
 * `@Align` from `kompute-serialization` and passing its generated `toByteArray()` result to [data]
 * instead of assembling the layout by hand.
 *
 * ```kotlin
 * val data = ByteBuffer.allocate(Float.SIZE_BYTES * 4 + Float.SIZE_BYTES)
 *     .order(ByteOrder.nativeOrder())
 *     .putFloat(centerX).putFloat(centerY).putFloat(centerZ)
 *     .putFloat(0f)    // std140 padding: vec3 occupies 16 bytes
 *     .putFloat(zoom)
 *     .array()
 * UniformBufferObject(0).data(data)
 * ```
 *
 * @property index the binding index in the shader — must be non-negative
 */
class UniformBufferObject(override val index: Int) : ShaderData, IndexedBinding {
    /**
     * Input data to upload to the GPU, or null if not set.
     */
    var data: ByteArray? = null
        private set

    /**
     * Sets the input data for this buffer.
     *
     * @param data the data to upload to the GPU
     * @return this [UniformBufferObject] for chaining
     */
    fun data(data: ByteArray): UniformBufferObject {
        this.data = data
        return this
    }

    /**
     * Validates the buffer configuration.
     *
     * @throws [KomputeConfigurationException] if the index is negative, [data] not provided
     */
    override fun validate() {
        super.validate()
        requireConfiguration(data != null) {
            "Data must be provided"
        }
    }

    override fun toString(): String = buildString {
        append("UniformBufferObject($index)")
        data?.let { append(".data([${it.size} bytes])") }
    }
}
