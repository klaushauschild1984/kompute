package de.hauschild.kompute.serialization.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * [SymbolProcessorProvider] for [GpuStructProcessor].
 */
class GpuStructProcessorProvider: SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = GpuStructProcessor(
        environment.codeGenerator, environment.logger
    )
}
