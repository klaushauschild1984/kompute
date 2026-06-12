package de.hauschild.kompute.core.pipeline

import de.hauschild.kompute.core.result.ShaderResult

/**
 * Executes a sequence of compute shader [Stage]s without CPU round-trips between them.
 *
 * Intermediate [de.hauschild.kompute.core.data.StorageBuffer]s passed across stages are kept
 * on the GPU — their handles are reused automatically and no read-back occurs until the final
 * stage result is accessed via [PipelineResult].
 *
 * Each stage's [de.hauschild.kompute.core.shader.CompiledShader] is closed immediately after
 * its dispatch. All [de.hauschild.kompute.core.result.ShaderResult]s are released when
 * [PipelineResult.close] is called.
 */
class Pipeline {
    private val results = mutableListOf<ShaderResult>()

    /**
     * Dispatches all [stages] in order and returns a [PipelineResult] backed by the last stage.
     *
     * Closing the returned [PipelineResult] releases all intermediate results and GPU resources
     * accumulated during this execution.
     *
     * @param stages the stages to execute in order — at least one required
     * @return the result of the last stage
     */
    @Suppress("SpreadOperator")
    fun execute(vararg stages: Stage): PipelineResult {
        stages.forEach { stage ->
            val result = stage.shader.dispatch(stage.x, stage.y, stage.z, *stage.data.toTypedArray())
            stage.close()
            results.add(result)
        }
        return PipelineResult(this, results.last())
    }

    /**
     * Releases all [ShaderResult]s accumulated during [execute].
     */
    internal fun close() {
        results.forEach { it.close() }
        results.clear()
    }
}
