package de.hauschild.kompute.core

/**
 * Holds the output data from a compute shader execution.
 *
 * Contains all storage buffers that were marked as output via [ShaderData.StorageBuffer.asOutput].
 * Use [storageBuffer] to retrieve computed results by their registered name.
 */
class ShaderResult(
    private val storageBuffer: Map<String, FloatArray>,
) {
    /**
     * Retrieves the output storage buffer with the given name.
     *
     * The name must match the one specified via [ShaderData.StorageBuffer.asOutput].
     *
     * @param name the output name to retrieve
     * @return the computed float data from the GPU
     * @throws IllegalStateException if no output with the given name exists
     */
    fun storageBuffer(name: String): FloatArray = storageBuffer[name] ?: error("No output named '$name'")
}
