package de.hauschild.kompute.core.shader

import de.hauschild.kompute.core.data.Binding
import de.hauschild.kompute.core.data.IndexedBinding
import de.hauschild.kompute.core.data.NamedBinding
import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.exception.requireConfiguration
import de.hauschild.kompute.core.logging.debugTimed
import de.hauschild.kompute.core.result.ShaderResult
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Base class for compiled shader implementations.
 *
 * Provides generic dispatch validation: workgroup counts ≥ 1, at least one
 * [de.hauschild.kompute.core.data.OutputCapable] output, no duplicate outputs, and
 * consistent binding indices/names within each binding type. Also provides uniform
 * DEBUG-level dispatch logging (input data summary and duration) for all backends,
 * so subclasses do not need to implement logging themselves.
 *
 * Subclasses override [validateDispatch] to add backend-specific limit checks (e.g.
 * maximum workgroup count queried from the GPU at runtime), and implement [doDispatch]
 * for the actual backend-specific dispatch.
 */
abstract class AbstractCompiledShader: CompiledShader {
    /**
     * Validates [data], logs a summary of it at DEBUG level, delegates to [doDispatch],
     * and logs the resulting duration at DEBUG level.
     *
     * @param x number of work groups in the X dimension
     * @param y number of work groups in the Y dimension
     * @param z number of work groups in the Z dimension
     * @param data shader inputs and outputs
     * @return the results of all output buffers after the dispatch completes
     */
    final override fun dispatch(
        x: Int,
        y: Int,
        z: Int,
        vararg data: ShaderData
    ): ShaderResult {
        validateDispatch(x, y, z, *data)
        val dataList = data.toList()
        logger.debug { "Dispatching ($x, $y, $z) with data: ${describe(dataList)}" }
        return logger.debugTimed({ duration -> "Dispatch completed in $duration" }) {
            doDispatch(x, y, z, dataList)
        }
    }

    /**
     * Performs the backend-specific dispatch.
     *
     * Called by [dispatch] after validation. Implementations submit [data] to the GPU
     * for the given workgroup grid and return the resulting [ShaderResult].
     *
     * @param x number of work groups in the X dimension
     * @param y number of work groups in the Y dimension
     * @param z number of work groups in the Z dimension
     * @param data shader inputs and outputs, already validated
     * @return the results of all output buffers after the dispatch completes
     */
    protected abstract fun doDispatch(
        x: Int,
        y: Int,
        z: Int,
        data: List<ShaderData>
    ): ShaderResult

    private fun describe(data: List<ShaderData>): String =
        data.groupingBy { it::class.simpleName }
            .eachCount()
            .entries
            .joinToString { (type, count) -> "$count x $type(s)" }

    /**
     * Validates the dispatch parameters before submitting work to the GPU.
     *
     * Checks that all workgroup counts are ≥ 1, at least one output is present,
     * no outputs are duplicated, and binding indices/names are consistent within
     * each binding type.
     *
     * @param x number of work groups in the X dimension
     * @param y number of work groups in the Y dimension
     * @param z number of work groups in the Z dimension
     * @param data shader inputs and outputs to validate
     * @throws KomputeConfigurationException if any constraint is violated
     */
    protected open fun validateDispatch(
        x: Int,
        y: Int,
        z: Int,
        vararg data: ShaderData
    ) {
        val workGroupCountValidationMessage = { "Work group count must be greater than or equal to one" }
        requireConfiguration(x >= 1, workGroupCountValidationMessage)
        requireConfiguration(y >= 1, workGroupCountValidationMessage)
        requireConfiguration(z >= 1, workGroupCountValidationMessage)

        requireConfiguration(data.isNotEmpty()) {
            "At least one data is required"
        }

        data.forEach { it.validate() }

        requireConfiguration(data.filterIsInstance<OutputCapable<*>>().any { it.isOutput }) {
            "At least one output is required"
        }

        val duplicates =
            data
                .filterIsInstance<OutputCapable<*>>()
                .groupBy { it }
                .filter { (_, occurrences) -> occurrences.size > 1 }
                .keys
        requireConfiguration(duplicates.isEmpty()) { "There are duplicated outputs: $duplicates" }

        data
            .filterIsInstance<Binding>()
            .groupBy { it::class }
            .forEach { (_, items) ->
                when (items.first()) {
                    is IndexedBinding -> IndexedBinding.crossValidate(items.filterIsInstance<IndexedBinding>())
                    is NamedBinding -> NamedBinding.crossValidate(items.filterIsInstance<NamedBinding>())
                }
            }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
