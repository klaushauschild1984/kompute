package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.requireConfiguration

/**
 * A uniform buffer that passes read-only configuration data from the CPU to the compute shader.
 *
 * UBOs are bound to a binding index declared in the shader source and follow the std140 memory
 * layout — `vec3` fields are aligned to 16 bytes and require manual padding in the data array.
 * Unlike [StorageBuffer], a uniform buffer cannot be written by the shader.
 *
 * @property index the binding index in the shader — must be non-negative
 */
class UniformBuffer(override val index: Int) : ShaderData, IndexedBinding {
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
     * @throws [KomputeConfigurationException] if the index is negative, [data] not provided
     */
    override fun validate() {
        super.validate()
        requireConfiguration(data != null) {
            "Data must be provided"
        }
    }

    override fun toString(): String = "UniformBuffer(index=$index)"
}
