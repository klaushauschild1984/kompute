package de.hauschild.kompute.serialization.processor

import de.hauschild.kompute.serialization.annotation.GpuStruct
import de.hauschild.kompute.serialization.annotation.Layout

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate

/**
 * [SymbolProcessor] that generates serializers for [GpuStruct] annotated classes.
 *
 * @param codeGenerator the [CodeGenerator] to use for generating the serializers
 * @param logger the [KSPLogger] to use for logging
 */
class GpuStructProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private val layout = GpuStructLayout(logger)
    private val codeGen = GpuStructCodeGenerator(layout)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(GpuStruct::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        val (ready, deferred) = symbols.partition { it.validate() }
        ready.forEach { generate(it) }
        return deferred
    }

    private fun generate(classDeclaration: KSClassDeclaration) {
        val annotation = classDeclaration.annotations
            .first { it.shortName.asString() == GpuStruct::class.simpleName }
        val layoutName = (annotation.arguments
            .first { it.name?.asString() == "layout" }
            .value as KSType).declaration.simpleName.asString()
        val memoryLayout = Layout.valueOf(layoutName)

        codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false, classDeclaration.containingFile!!),
            packageName = classDeclaration.packageName.asString(),
            fileName = "${classDeclaration.simpleName.asString()}Serializer",
        ).use { output ->
            output.write(codeGen.generate(classDeclaration, memoryLayout).toByteArray())
        }
    }
}
