package de.hauschild.kompute.core

import io.github.oshai.kotlinlogging.KotlinLogging

abstract class AbstractBackend : Backend {
    protected val logger = KotlinLogging.logger {}

    private var initialized = false

    @InternalApi
    override fun initialize() {
        if (initialized) {
            throw IllegalStateException("Backend already initialized")
        }
        logger.debug { "Initializing ${this::class.simpleName} v${BuildInfo.VERSION}" }
        doInitialize()
        initialized = true
    }

    abstract fun doInitialize()

    override fun shader(source: ShaderSource): ShaderBuilder {
        val context = ExecutionContext(source)
        return ShaderBuilder(context) { ctx -> execute(ctx) }
    }

    abstract fun execute(context: ExecutionContext): ShaderResult
}
