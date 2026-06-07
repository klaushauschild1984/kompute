package de.hauschild.kompute.core.backend

import de.hauschild.kompute.core.BuildInfo
import de.hauschild.kompute.core.exception.KomputeBackendDispatchException
import de.hauschild.kompute.core.exception.KomputeException
import de.hauschild.kompute.core.exception.requireBackendInitialization
import de.hauschild.kompute.core.execution.ExecutionContext
import de.hauschild.kompute.core.execution.ShaderBuilder
import de.hauschild.kompute.core.execution.ShaderResult
import de.hauschild.kompute.core.execution.ShaderSource
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Base class for GPU compute backend implementations.
 *
 * Handles common initialization logic and wires the shader/dispatch/execute pipeline.
 * Subclasses provide the platform-specific GPU implementation.
 *
 * Subclasses must implement:
 * - [doInitialize] for backend-specific setup
 * - [dispatch] for the actual GPU computation
 */
abstract class AbstractBackend : Backend {
    /**
     * Logger available to subclasses for debugging and diagnostics.
     */
    protected val logger = KotlinLogging.logger {}
    private var initialized = false

    /**
     * Initializes the backend exactly once.
     *
     * Delegates to [doInitialize] for backend-specific setup.
     *
     * @throws [KomputeBackendInitializationException] if called more than once
     */
    @InternalApi
    override fun initialize() {
        requireBackendInitialization(!initialized) { "Backend already initialized" }
        logger.debug { "Initializing ${this::class.simpleName} v${BuildInfo.VERSION}" }
        doInitialize()
        initialized = true
    }

    /**
     * Performs backend-specific initialization.
     *
     * Called once by [initialize]. Implementations should validate GPU capabilities
     * and set up required resources.
     *
     * @throws [KomputeBackendInitializationException] if GPU resources are unavailable or initialization fails
     */
    abstract fun doInitialize()

    /**
     * Creates a shader computation pipeline for the given shader source.
     *
     * @param source the compute shader source to use
     * @return a [ShaderBuilder] for configuring shader data
     */
    @Suppress("TooGenericExceptionCaught")
    override fun shader(source: ShaderSource): ShaderBuilder {
        val context = ExecutionContext(source)
        return ShaderBuilder(context) { ctx ->
            try {
                dispatch(ctx)
            } catch (komputeException: KomputeException) {
                throw komputeException
            } catch (exception: Exception) {
                throw KomputeBackendDispatchException("Unexpected error during GPU dispatch", exception)
            }
        }
    }

    /**
     * Dispatches the execution of the configured compute shader on the GPU.
     *
     * Called by [ExecutionBuilder.execute]. Implementations compile and link the shader,
     * bind buffers, dispatch the computation, and return results.
     *
     * @param context the execution context with shader source, data, and dispatch dimensions
     * @return a [ShaderResult] containing all output buffers
     * @throws KomputeBackendDispatchException if GPU execution fails
     */
    abstract fun dispatch(context: ExecutionContext): ShaderResult
}
