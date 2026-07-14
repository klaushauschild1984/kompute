package de.hauschild.kompute.serialization.processor

import de.hauschild.kompute.serialization.GpuStructDeserializer
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
 * Generates the `fromByteArray()` extension function on [ByteArray] for [KSClassDeclaration]s
 * annotated with [de.hauschild.kompute.serialization.annotation.GpuStruct].
 *
 * Mirrors [ToByteArrayCodeGenerator] in reverse: reads fields at their statically known offsets
 * and constructs the target type via its primary constructor. No runtime reflection is used —
 * `@GpuField`/`@GpuStruct` have `SOURCE` retention and would be invisible at runtime regardless.
 *
 * @param layout used to compute field offsets, sizes, and padding
 */
class FromByteArrayCodeGenerator(private val layout: GpuStructLayout) {
    /**
     * Generates the `fromByteArray()` extension function for [classDeclaration].
     *
     * @param classDeclaration the [de.hauschild.kompute.serialization.annotation.GpuStruct] annotated class
     * @return the generated extension function
     */
    fun generate(classDeclaration: KSClassDeclaration): FunSpec {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val properties = layout.gpuFields(classDeclaration)
        layout.validateConstructorFields(classDeclaration, properties)
        val layoutDependent = layout.isLayoutDependent(properties)

        val deserializerClass = GpuStructDeserializer::class.asClassName()
        val targetClass = ClassName(packageName, className)
        val layoutClass = Layout::class.asClassName()

        val funSpecBuilder = FunSpec.builder("fromByteArray")
            .receiver(ByteArray::class)
            .addParameter("type", KClass::class.asClassName().parameterizedBy(targetClass))
            .returns(targetClass)

        val bodyBuilder = CodeBlock.builder()

        if (!layoutDependent) {
            val fieldLayouts = layout.computeLayout(properties, Layout.STD140)
            bodyBuilder.addStatement("val reader = %T(this)", deserializerClass)
            bodyBuilder.add("return ")
            bodyBuilder.add(constructorCall(targetClass, fieldLayouts))
            bodyBuilder.add("\n")
        } else {
            funSpecBuilder.addParameter(
                ParameterSpec.builder("layout", layoutClass)
                    .defaultValue("%T.STD140", layoutClass)
                    .build()
            )
            bodyBuilder.beginControlFlow("return when (layout)")
            for (memLayout in Layout.entries) {
                val fieldLayouts = layout.computeLayout(properties, memLayout)
                bodyBuilder.beginControlFlow("%T.%L ->", layoutClass, memLayout)
                bodyBuilder.addStatement("val reader = %T(this)", deserializerClass)
                bodyBuilder.add(constructorCall(targetClass, fieldLayouts))
                bodyBuilder.add("\n")
                bodyBuilder.endControlFlow()
            }
            bodyBuilder.endControlFlow()
        }

        return funSpecBuilder
            .addCode(bodyBuilder.build())
            .build()
    }

    private fun constructorCall(
        targetClass: ClassName,
        fieldLayouts: List<GpuStructLayout.FieldLayout>,
    ): CodeBlock {
        val builder = CodeBlock.builder().add("%T(", targetClass)
        fieldLayouts.forEachIndexed { index, field ->
            if (index > 0) {
                builder.add(", ")
            }
            builder.add("%L = ", field.property.simpleName.asString())
            builder.add(fieldExpr(field))
        }
        return builder.add(")").build()
    }

    private fun fieldExpr(field: GpuStructLayout.FieldLayout): CodeBlock =
        when {
            field.isArray() && field.isElementGpuStruct() -> {
                val elementDeclaration = (field.descriptor as StructArrayDescriptor).elementDeclaration
                val elementType = ClassName(
                    elementDeclaration.packageName.asString(),
                    elementDeclaration.simpleName.asString(),
                )
                CodeBlock.of(
                    "Array(%L) { i -> this.copyOfRange(%L, %L).fromByteArray(%T::class) }",
                    elementCountExpr(field),
                    "${field.offset} + i * ${field.elementStride}",
                    "${field.offset} + i * ${field.elementStride} + ${field.elementSize}",
                    elementType,
                )
            }
            field.isArray() -> {
                val (arrayType, readFunctionName) = primitiveArrayInfo(field.property)
                CodeBlock.of(
                    "%T(%L) { i -> reader.%L(%L) }",
                    arrayType,
                    elementCountExpr(field),
                    readFunctionName,
                    "${field.offset} + i * ${field.elementStride}",
                )
            }
            field.isGpuStructField() -> {
                val declaration = (field.descriptor as GpuStructDescriptor).declaration
                val nestedType = ClassName(declaration.packageName.asString(), declaration.simpleName.asString())
                CodeBlock.of(
                    "this.copyOfRange(%L, %L).fromByteArray(%T::class)",
                    field.offset,
                    field.offset + field.size,
                    nestedType,
                )
            }
            else -> CodeBlock.of("reader.%L(%L)", primitiveReadFunctionName(field.property), field.offset)
        }

    /**
     * Returns the Kotlin expression for the array's element count: the declared
     * [de.hauschild.kompute.serialization.annotation.FixedSize] value for a fixed-size array, or an
     * expression deriving the count from the remaining byte array length for a dynamically sized one.
     *
     * @param field the array field to compute the element count expression for
     * @return the element count as a Kotlin source expression
     */
    private fun elementCountExpr(field: GpuStructLayout.FieldLayout): String =
        field.fixedElementCount()?.toString() ?: "(this.size - ${field.offset}) / ${field.elementStride}"

    private fun primitiveReadFunctionName(property: KSPropertyDeclaration): String {
        val type = property.type.resolve()
        val qualifiedName = type.declaration.qualifiedName?.asString()
        return when (qualifiedName) {
            Int::class.qualifiedName -> "readInt"
            Float::class.qualifiedName -> "readFloat"
            Boolean::class.qualifiedName -> "readBoolean"
            else -> error("Unsupported scalar type for field '${property.simpleName.asString()}'")
        }
    }

    private fun primitiveArrayInfo(property: KSPropertyDeclaration): Pair<ClassName, String> {
        val type = property.type.resolve()
        val qualifiedName = type.declaration.qualifiedName?.asString()
        return when (qualifiedName) {
            "kotlin.FloatArray" -> FloatArray::class.asClassName() to "readFloat"
            "kotlin.IntArray" -> IntArray::class.asClassName() to "readInt"
            "kotlin.BooleanArray" -> BooleanArray::class.asClassName() to "readBoolean"
            else -> error("Unsupported array type for field '${property.simpleName.asString()}'")
        }
    }
}
