package de.hauschild.kompute.core

import de.hauschild.kompute.core.ShaderData.OutputCapable
import de.hauschild.kompute.core.ShaderData.StorageBuffer

/**
 * Attaches input and output data to a compute shader.
 *
 * Each [ShaderData] is self-validated before being accepted. Proceed to [DispatchBuilder]
 * after configuring all data.
 */
class ShaderBuilder(
    private val context: ExecutionContext,
    private val executor: (ExecutionContext) -> ShaderResult,
) {
    /**
     * Attaches one or more shader data objects to this computation.
     *
     * Each item is validated immediately via [ShaderData.validate].
     *
     * @param data the shader data to attach (storage buffers, etc.)
     * @return a [DispatchBuilder] to configure the compute grid dimensions
     * @throws KomputeConfigurationException if any item fails validation
     */
    fun data(vararg data: ShaderData): DispatchBuilder {
        requireConfiguration(data.isNotEmpty()) {
            "At least one data is required"
        }

        data.forEach { it.validate() }

        requireConfiguration(data.filterIsInstance<OutputCapable>().any { it.isOutput() }) {
            "At least one output is required"
        }

        val duplicates =
            data
                .filterIsInstance<OutputCapable>()
                .mapNotNull { it.outputName }
                .groupBy { it }
                .filter { (_, occurrences) -> occurrences.size > 1 }
                .keys
        requireConfiguration(duplicates.isEmpty()) { "There are duplicated output names: $duplicates" }

        data
            .groupBy { it::class }
            .forEach { (_, items) ->
                when (items.first()) {
                    is StorageBuffer<*> -> {
                        val storageBuffers = items.filterIsInstance<StorageBuffer<*>>()
                        StorageBuffer.crossValidate(storageBuffers)
                    }
                }
            }

        context.data.addAll(data.toList())

        return DispatchBuilder(context, executor)
    }
}
