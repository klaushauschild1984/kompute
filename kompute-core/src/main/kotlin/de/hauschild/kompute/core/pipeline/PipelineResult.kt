package de.hauschild.kompute.core.pipeline

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.result.ShaderResult

/**
 * The result of a [Pipeline] execution, backed by the last stage's [ShaderResult].
 *
 * Closing this result releases all intermediate [ShaderResult]s and GPU resources
 * accumulated across all pipeline stages. Always use this in a `use {}` block or
 * call [close] explicitly when done.
 *
 * @param pipeline the pipeline whose resources are released on [close]
 * @param result the [ShaderResult] of the last pipeline stage
 */
class PipelineResult(
    private val pipeline: Pipeline,
    private val result: ShaderResult,
) : AutoCloseable {
    /**
     * Retrieves the output data for the given [OutputCapable] from the last pipeline stage.
     *
     * @param output the output buffer to retrieve the result for
     * @return the data produced by the last stage for the given output
     */
    operator fun <T : Any> get(output: OutputCapable<T>): T = result[output]

    override fun close() {
        pipeline.close()
    }
}
