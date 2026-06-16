package de.hauschild.kompute.serialization.processor

import de.hauschild.kompute.serialization.GpuStructSerializer
import de.hauschild.kompute.serialization.annotation.Layout
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Generates the `toByteArray()` extension function for [KSClassDeclaration]s
 * annotated with [de.hauschild.kompute.serialization.annotation.GpuStruct].
 *
 * @param layout used to compute field offsets, sizes, and padding
 */
class GpuStructCodeGenerator(private val layout: GpuStructLayout) {
    /**
     * Generates the `toByteArray()` extension function source for [classDeclaration].
     *
     * @param classDeclaration the [de.hauschild.kompute.serialization.annotation.GpuStruct] annotated class
     * @return Kotlin source code of the generated extension function
     */
    fun generate(classDeclaration: KSClassDeclaration): String {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val properties = layout.gpuFields(classDeclaration)
        val layoutDependent = layout.isLayoutDependent(properties)

        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import ${GpuStructSerializer::class.qualifiedName}")
            if (layoutDependent) {
                appendLine("import ${Layout::class.qualifiedName}")
            }
            appendLine()
            if (!layoutDependent) {
                val fieldLayouts = layout.computeLayout(properties, Layout.STD140)
                val (bufferSizeExpr, trailingPadding) = computeBufferSizing(fieldLayouts, properties, Layout.STD140)
                appendLine("fun $className.toByteArray(): ByteArray {")
                append(generateBody(fieldLayouts, trailingPadding, bufferSizeExpr))
                appendLine("}")
            } else {
                appendLine("fun $className.toByteArray(layout: Layout = Layout.STD140): ByteArray = when (layout) {")
                for (memLayout in Layout.entries) {
                    val fieldLayouts = layout.computeLayout(properties, memLayout)
                    val (bufferSizeExpr, trailingPadding) = computeBufferSizing(fieldLayouts, properties, memLayout)
                    appendLine("    Layout.$memLayout -> {")
                    append(
                        generateBody(fieldLayouts, trailingPadding, bufferSizeExpr, useReturn = false)
                            .trimEnd()
                            .prependIndent("    ")
                    )
                    appendLine()
                    appendLine("    }")
                }
                appendLine("}")
            }
        }
    }

    private fun generateBody(
        fieldLayouts: List<GpuStructLayout.FieldLayout>,
        trailingPadding: Int,
        bufferSizeExpr: String,
        useReturn: Boolean = true,
    ): String = buildString {
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
        if (useReturn) {
            appendLine("    return writer.toByteArray()")
        } else {
            appendLine("    writer.toByteArray()")
        }
    }

    private fun computeBufferSizing(
        fieldLayouts: List<GpuStructLayout.FieldLayout>,
        properties: List<KSPropertyDeclaration>,
        memoryLayout: Layout,
    ): BufferSizing {
        val arrayField = fieldLayouts.lastOrNull { it.isArray() }
        val staticSize = fieldLayouts.filter { !it.isArray() }.lastOrNull()?.let { it.offset + it.size } ?: 0
        val trailing = layout.trailingPadding(staticSize, properties, memoryLayout)
        val sizeExpr = arrayField?.let {
            val dynamic = "this.${it.property.simpleName.asString()}.size * ${it.elementStride}"
            if (it.offset > 0) "${it.offset} + $dynamic" else dynamic
        } ?: "${staticSize + trailing}"
        return BufferSizing(sizeExpr, trailing)
    }

    /**
     * @property sizeExpr
     * @property trailingPadding
     */
    private data class BufferSizing(val sizeExpr: String, val trailingPadding: Int)
}
