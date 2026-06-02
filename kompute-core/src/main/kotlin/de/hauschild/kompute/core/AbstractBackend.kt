package de.hauschild.kompute.core

import io.github.oshai.kotlinlogging.KotlinLogging

abstract class AbstractBackend : Backend {
    protected val logger = KotlinLogging.logger {}

    private var initialized = false

    override fun initialize() {
        if (initialized) {
            throw IllegalStateException("Backend already initialized")
        }
        logger.debug { "Initializing ${this::class.simpleName}" }
        doInitialize()
        initialized = true
    }

    abstract fun doInitialize()
}
