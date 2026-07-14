package de.hauschild.kompute.serialization.processor

import de.hauschild.kompute.serialization.GpuStructSerializer
import de.hauschild.kompute.serialization.annotation.Layout
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asClassName

/**
 * Generates the `toByteArray()` extension function for [KSClassDeclaration]s
 * annotated with [de.hauschild.kompute.serialization.annotation.GpuStruct].
 *
 * @param layout used to compute field offsets, sizes, and padding
 */
class ToByteArrayCodeGenerator(private val layout: GpuStructLayout) {
    /**
     * Generates the `toByteArray()` extension function for [classDeclaration].
     *
     * @param classDeclaration the [de.hauschild.kompute.serialization.annotation.GpuStruct] annotated class
     * @return the generated extension function
     */
    fun generate(classDeclaration: KSClassDeclaration): FunSpec {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val properties = layout.gpuFields(classDeclaration)
        val layoutDependent = layout.isLayoutDependent(properties)

        val serializerClass = GpuStructSerializer::class.asClassName()
        val receiverClass = ClassName(packageName, className)
        val layoutClass = Layout::class.asClassName()

        val bodyBuilder = CodeBlock.builder()
        val funSpecBuilder = FunSpec.builder("toByteArray")
            .receiver(receiverClass)
            .returns(ByteArray::class)

        if (!layoutDependent) {
            val fieldLayouts = layout.computeLayout(properties, Layout.STD140)
            val (bufferSizeExpr, trailingPadding) = computeBufferSizing(fieldLayouts, properties, Layout.STD140)
            bodyBuilder.addStatement("val writer = %T(%L)", serializerClass, bufferSizeExpr)
            generateBody(bodyBuilder, fieldLayouts, trailingPadding)
            bodyBuilder.addStatement("return writer.toByteArray()")
        } else {
            funSpecBuilder.addParameter(
                ParameterSpec.builder("layout", layoutClass)
                    .defaultValue("%T.STD140", layoutClass)
                    .build()
            )
            bodyBuilder.beginControlFlow("return when (layout)")
            for (memLayout in Layout.entries) {
                val fieldLayouts = layout.computeLayout(properties, memLayout)
                val (bufferSizeExpr, trailingPadding) = computeBufferSizing(fieldLayouts, properties, memLayout)
                bodyBuilder.beginControlFlow("%T.%L ->", layoutClass, memLayout)
                bodyBuilder.addStatement("val writer = %T(%L)", serializerClass, bufferSizeExpr)
                generateBody(bodyBuilder, fieldLayouts, trailingPadding)
                bodyBuilder.addStatement("writer.toByteArray()")
                bodyBuilder.endControlFlow()
            }
            bodyBuilder.endControlFlow()
        }

        return funSpecBuilder
            .addCode(bodyBuilder.build())
            .build()
    }

    private fun generateBody(
        codeBlockBuilder: CodeBlock.Builder,
        fieldLayouts: List<GpuStructLayout.FieldLayout>,
        trailingPadding: Int,
    ) {
        fieldLayouts.forEach { field ->
            if (field.padding > 0) {
                codeBlockBuilder.addStatement("writer.skip(%L)", field.padding)
            }
            if (field.isArray()) {
                field.fixedElementCount()?.let { fixedElementCount ->
                    val fieldName = field.property.simpleName.asString()
                    codeBlockBuilder.addStatement(
                        "require(this.%L.size == %L) " +
                                "{ \"Expected %L elements for '%L', but was \${this.%L.size}\" }",
                        fieldName, fixedElementCount, fixedElementCount, fieldName, fieldName,
                    )
                }
                codeBlockBuilder.beginControlFlow("for (element in this.%L)", field.property.simpleName.asString())
                if (field.isElementGpuStruct()) {
                    codeBlockBuilder.addStatement("writer.write(element.toByteArray())")
                } else {
                    codeBlockBuilder.addStatement("writer.write(element)")
                }
                if (field.interElementPadding() > 0) {
                    codeBlockBuilder.addStatement("writer.skip(%L)", field.interElementPadding())
                }
                codeBlockBuilder.endControlFlow()
            } else if (field.isGpuStructField()) {
                codeBlockBuilder.addStatement("writer.write(this.%L.toByteArray())",
                    field.property.simpleName.asString())
            } else {
                codeBlockBuilder.addStatement("writer.write(this.%L)", field.property.simpleName.asString())
            }
        }
        if (trailingPadding > 0) {
            codeBlockBuilder.addStatement("writer.skip(%L)", trailingPadding)
        }
    }

    private fun computeBufferSizing(
        fieldLayouts: List<GpuStructLayout.FieldLayout>,
        properties: List<KSPropertyDeclaration>,
        memoryLayout: Layout,
    ): BufferSizing {
        val dynamicField = fieldLayouts.lastOrNull { it.isDynamicArray() }
        val staticSize = fieldLayouts.lastOrNull { !it.isDynamicArray() }?.let { it.offset + it.size } ?: 0
        val trailing = layout.trailingPadding(staticSize, properties, memoryLayout)
        val sizeExpr = dynamicField?.let {
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
