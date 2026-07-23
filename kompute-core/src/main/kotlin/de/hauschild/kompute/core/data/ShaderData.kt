package de.hauschild.kompute.core.data

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
}
