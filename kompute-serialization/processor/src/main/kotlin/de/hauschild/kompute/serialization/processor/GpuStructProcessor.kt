package de.hauschild.kompute.serialization.processor

import de.hauschild.kompute.serialization.annotation.GpuStruct

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec

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
    private val toByteArrayGenerator = ToByteArrayCodeGenerator(layout)
    private val sizeOfGenerator = SizeOfCodeGenerator(layout)
    private val fromByteArrayGenerator = FromByteArrayCodeGenerator(layout)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(GpuStruct::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

        val (ready, deferred) = symbols.partition { it.validate() }
        ready.forEach { generate(it) }
        return deferred
    }

    private fun generate(classDeclaration: KSClassDeclaration) {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val fileSpec = try {
            FileSpec.builder(packageName, className)
                .addFunction(toByteArrayGenerator.generate(classDeclaration))
                .addFunction(sizeOfGenerator.generate(classDeclaration))
                .addFunction(fromByteArrayGenerator.generate(classDeclaration))
                .build()
        } catch (exception: IllegalStateException) {
            // an invalid layout (e.g. a nested dynamically sized array) was already reported via
            // logger.error() in GpuStructLayout; skip generation instead of crashing the KSP round
            logger.warn("Skipping codegen for '$className': ${exception.message}")
            return
        }
        codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false, classDeclaration.containingFile!!),
            packageName = packageName,
            fileName = "${className}Serializer",
        ).use { output ->
            output.write(fileSpec.toString().toByteArray())
        }
    }
}
