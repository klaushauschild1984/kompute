package de.hauschild.kompute.core.backend

import de.hauschild.kompute.core.BuildInfo
import de.hauschild.kompute.core.InternalApi
import de.hauschild.kompute.core.exception.KomputeBackendInitializationException
import de.hauschild.kompute.core.exception.KomputeException
import de.hauschild.kompute.core.exception.requireBackendInitialization
import de.hauschild.kompute.core.shader.CompiledShader
import de.hauschild.kompute.core.shader.ShaderBuilder
import de.hauschild.kompute.core.shader.ShaderSource
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
     * @throws [de.hauschild.kompute.core.exception.KomputeBackendInitializationException] if called more than once
     */
    @InternalApi
    override fun initialize() {
        requireBackendInitialization(!initialized) { "Backend already initialized" }
        logger.debug { "Initializing ${this::class.simpleName} v${BuildInfo.VERSION}" }
        @Suppress("TooGenericExceptionCaught")
        try {
            doInitialize()
        } catch (exception: KomputeException) {
            throw exception
        } catch (exception: Exception) {
            throw KomputeBackendInitializationException("Failed to initialize backend", exception)
        }
        initialized = true
    }

    /**
     * Performs backend-specific initialization.
     *
     * Called once by [initialize]. Implementations should validate GPU capabilities
     * and set up required resources.
     *
     * @throws [de.hauschild.kompute.core.exception.KomputeBackendInitializationException] if GPU resources are
     * unavailable or initialization fails
     */
    abstract fun doInitialize()

    /**
     * Creates a shader computation pipeline for the given shader source.
     *
     * @param source the compute shader source to use
     * @return a [ShaderBuilder] for configuring shader data
     */
    override fun shader(source: ShaderSource): ShaderBuilder = ShaderBuilder(source, ::compileSource)

    /**
     * Compiles the given shader source into a backend-specific [de.hauschild.kompute.core.shader.CompiledShader].
     *
     * @param source the compute shader source to compile
     * @return the compiled and linked [de.hauschild.kompute.core.shader.CompiledShader]
     */
    protected abstract fun compileSource(source: ShaderSource): CompiledShader
}
