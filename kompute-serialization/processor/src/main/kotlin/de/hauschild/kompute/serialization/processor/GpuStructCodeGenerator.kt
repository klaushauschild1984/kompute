package de.hauschild.kompute.serialization.processor

import de.hauschild.kompute.serialization.GpuStructSerializer
import de.hauschild.kompute.serialization.annotation.Layout
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Generates the `toByteArray()` extension function for [KSClassDeclaration]s
 * annotated with [de.hauschild.kompute.serialization.annotation.GpuStruct].
 *
 * @param layout used to compute field offsets, sizes, and padding
 */
class GpuStructCodeGenerator(private val layout: GpuStructLayout) {
    /**
     * Generates the `toByteArray()` extension function source for [classDeclaration]
     * using [memoryLayout] alignment rules.
     *
     * @param classDeclaration the [de.hauschild.kompute.serialization.annotation.GpuStruct] annotated class
     * @param memoryLayout the memory layout standard to apply
     * @return Kotlin source code of the generated extension function
     */
    fun generate(classDeclaration: KSClassDeclaration, memoryLayout: Layout): String {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val properties = layout.gpuFields(classDeclaration)
        val fieldLayouts = layout.computeLayout(properties, memoryLayout)

        val arrayField = fieldLayouts.lastOrNull { it.isArray() }
        val staticBufferSize = fieldLayouts.filter { !it.isArray() }.lastOrNull()?.let { it.offset + it.size } ?: 0
        val trailingPadding = layout.trailingPadding(staticBufferSize, properties, memoryLayout)
        val bufferSizeExpr = arrayField?.let {
            val dynamicPart = "this.${arrayField.property.simpleName.asString()}.size * ${arrayField.elementStride}"
            if (arrayField.offset > 0) "${arrayField.offset} + $dynamicPart" else dynamicPart
        } ?: "${staticBufferSize + trailingPadding}"

        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import ${GpuStructSerializer::class.qualifiedName}")
            appendLine()
            appendLine("fun $className.toByteArray(): ByteArray {")
            appendLine("    val writer = ${GpuStructSerializer::class.simpleName}($bufferSizeExpr)")
            fieldLayouts.forEach { field ->
                if (field.padding > 0) {
                    appendLine("    writer.skip(${field.padding})")
                }
                if (field.isArray()) {
                    appendLine("    this.${field.property.simpleName.asString()}.forEach { element ->")
                    if (field.isElementGpuStruct()) {
                        appendLine("        writer.write(element.toByteArray())")
                    } else {
                        appendLine("        writer.write(element)")
                    }
                    if (field.interElementPadding() > 0) {
                        appendLine("        writer.skip(${field.interElementPadding()})")
                    }
                    appendLine("    }")
                } else if (field.isGpuStructField()) {
                    appendLine("    writer.write(this.${field.property.simpleName.asString()}.toByteArray())")
                } else {
                    appendLine("    writer.write(this.${field.property.simpleName.asString()})")
                }
            }
            if (trailingPadding > 0) {
                appendLine("    writer.skip($trailingPadding)")
            }
            appendLine("    return writer.toByteArray()")
            appendLine("}")
        }
    }
}
