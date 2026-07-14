package de.hauschild.kompute.serialization.processor

import de.hauschild.kompute.serialization.annotation.Layout
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import kotlin.reflect.KClass

/**
 * Generates the `sizeOf()` extension function on `KClass<T>` for [KSClassDeclaration]s
 * annotated with [de.hauschild.kompute.serialization.annotation.GpuStruct].
 *
 * There is no instance to serialize, so the struct is identified by its [KClass] rather than
 * by a receiver instance, mirroring how [ToByteArrayCodeGenerator] resolves via a concrete
 * receiver type, without runtime reflection.
 *
 * @param layout used to compute field offsets, sizes, and padding
 */
class SizeOfCodeGenerator(private val layout: GpuStructLayout) {
    /**
     * Generates the `sizeOf()` extension function for [classDeclaration].
     *
     * @param classDeclaration the [de.hauschild.kompute.serialization.annotation.GpuStruct] annotated class
     * @return the generated extension function
     */
    fun generate(classDeclaration: KSClassDeclaration): FunSpec {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val properties = layout.gpuFields(classDeclaration)
        val layoutDependent = layout.isLayoutDependent(properties)
        val arrayTerminated = layout.computeLayout(properties, Layout.STD140).lastOrNull()?.isArray() == true

        val receiverClass = ClassName(packageName, className)
        val typeToken = KClass::class.asClassName().parameterizedBy(receiverClass)
        val layoutClass = Layout::class.asClassName()

        val funSpecBuilder = FunSpec.builder("sizeOf")
            .receiver(typeToken)
            .returns(Int::class)

        if (arrayTerminated) {
            funSpecBuilder.addParameter("elementCount", Int::class)
        }

        val bodyBuilder = CodeBlock.builder()
        if (!layoutDependent) {
            val fieldLayouts = layout.computeLayout(properties, Layout.STD140)
            bodyBuilder.addStatement("return %L", sizeExpr(fieldLayouts, properties, Layout.STD140))
        } else {
            funSpecBuilder.addParameter(
                ParameterSpec.builder("layout", layoutClass)
                    .defaultValue("%T.STD140", layoutClass)
                    .build()
            )
            bodyBuilder.beginControlFlow("return when (layout)")
            for (memLayout in Layout.entries) {
                val fieldLayouts = layout.computeLayout(properties, memLayout)
                val expr = sizeExpr(fieldLayouts, properties, memLayout)
                bodyBuilder.addStatement("%T.%L -> %L", layoutClass, memLayout, expr)
            }
            bodyBuilder.endControlFlow()
        }

        return funSpecBuilder
            .addCode(bodyBuilder.build())
            .build()
    }

    private fun sizeExpr(
        fieldLayouts: List<GpuStructLayout.FieldLayout>,
        properties: List<KSPropertyDeclaration>,
        memoryLayout: Layout,
    ): String {
        val arrayField = fieldLayouts.lastOrNull { it.isArray() }
        val staticSize = fieldLayouts.lastOrNull { !it.isArray() }?.let { it.offset + it.size } ?: 0
        val trailing = layout.trailingPadding(staticSize, properties, memoryLayout)
        return arrayField?.let {
            val dynamic = "elementCount * ${it.elementStride}"
            if (it.offset > 0) "${it.offset} + $dynamic" else dynamic
        } ?: "${staticSize + trailing}"
    }
}
