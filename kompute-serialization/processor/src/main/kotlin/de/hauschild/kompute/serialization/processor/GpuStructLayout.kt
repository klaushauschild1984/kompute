package de.hauschild.kompute.serialization.processor

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.FixedSize
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct
import de.hauschild.kompute.serialization.annotation.Layout
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType

/**
 * Computes the memory layout of [GpuStruct] annotated classes according to std140 and std430 rules.
 *
 * @param logger used to report unsupported field types as compiler errors
 */
@Suppress("TooManyFunctions")
class GpuStructLayout(private val logger: KSPLogger) {
    private val primitiveArrayTypes = setOf("kotlin.FloatArray", "kotlin.IntArray", "kotlin.BooleanArray")
    private val genericArrayType = "kotlin.Array"

    /**
     * Returns all properties of [classDeclaration] annotated with [GpuField].
     *
     * @param classDeclaration the class to inspect
     * @return list of properties annotated with [GpuField], in declaration order
     */
    fun gpuFields(classDeclaration: KSClassDeclaration): List<KSPropertyDeclaration> =
        classDeclaration.getAllProperties()
            .filter { property ->
                property.annotations.any { it.shortName.asString() == GpuField::class.simpleName!! }
            }
            .toList()

    /**
     * Returns true if [declaration] is annotated with [GpuStruct].
     *
     * @param declaration the class declaration to check
     * @return true if [GpuStruct] is present on [declaration]
     */
    fun isGpuStruct(declaration: KSClassDeclaration): Boolean =
        declaration.annotations.any { it.shortName.asString() == GpuStruct::class.simpleName }

    /**
     * Computes the [FieldLayout] for each property, respecting alignment and inter-field padding
     * according to [layout].
     *
     * A dynamically sized array field (no [FixedSize]) must appear last; a compiler error is
     * emitted otherwise. Such a field has [FieldLayout.size] of 0 (dynamic at runtime) but still
     * carries [FieldLayout.elementStride]. A [FixedSize] array field has a real, statically known
     * [FieldLayout.size] and may appear anywhere in the struct.
     *
     * @param properties the [GpuField]-annotated properties to lay out
     * @param layout the memory layout standard to apply
     * @return list of [FieldLayout] entries with computed offsets and padding
     */
    fun computeLayout(properties: List<KSPropertyDeclaration>, layout: Layout): List<FieldLayout> {
        var offset = 0
        return properties.mapIndexed { index, property ->
            val descriptor = descriptorFor(property)
            if (descriptor.isDynamic() && index != properties.lastIndex) {
                logger.error("Dynamically sized array fields must be the last field in a @GpuStruct", property)
            }
            val alignment = descriptor.alignment(layout)
            val padding = (alignment - (offset % alignment)) % alignment
            offset += padding
            if (descriptor.isArray()) {
                val size = descriptor.size(layout)
                FieldLayout(property, offset, size, padding, descriptor, descriptor.elementSize(layout),
                    descriptor.elementStride(layout)).also { offset += size }
            } else {
                val size = descriptor.size(layout)
                FieldLayout(property, offset, size, padding, descriptor).also { offset += size }
            }
        }
    }

    /**
     * Returns the number of trailing padding bytes needed to align [rawSize] to the struct's
     * base alignment (maximum member alignment).
     *
     * Returns 0 when the last property is a dynamically sized array field, since its size is
     * only known at runtime.
     *
     * @param rawSize the unpadded size of the struct in bytes
     * @param properties the struct's [GpuField]-annotated properties
     * @param layout the memory layout standard to apply
     * @return number of trailing padding bytes, 0 if already aligned or last field is a dynamic array
     */
    fun trailingPadding(
        rawSize: Int,
        properties: List<KSPropertyDeclaration>,
        layout: Layout
    ): Int {
        val descriptors = properties.map { descriptorFor(it) }
        if (descriptors.lastOrNull()?.isDynamic() == true) {
            return 0
        }
        val alignment = descriptors.maxOfOrNull { it.alignment(layout) } ?: 1
        return (alignment - (rawSize % alignment)) % alignment
    }

    /**
     * Determines whether the layout of the struct is dependent on the values of the given properties.
     *
     * @param properties the properties to check
     */
    fun isLayoutDependent(properties: List<KSPropertyDeclaration>): Boolean =
        properties.any { descriptorFor(it).isLayoutDependent() }

    /**
     * Computes the total serialized size in bytes of [declaration] under [layout], including trailing padding.
     *
     * Returns 0 for the dynamic portion of a trailing array field, since its length is only known at runtime;
     * see [SizeOfCodeGenerator] for how the array case is handled at the call site.
     *
     * @param declaration the struct's class declaration
     * @param layout the memory layout standard to apply
     * @return total size in bytes
     */
    fun structSizeOf(declaration: KSClassDeclaration, layout: Layout): Int {
        val innerProperties = gpuFields(declaration)
        val innerLayout = computeLayout(innerProperties, layout)
        val rawSize = innerLayout.lastOrNull()?.let { it.offset + it.size } ?: 0
        return rawSize + trailingPadding(rawSize, innerProperties, layout)
    }

    /**
     * Validates that every [GpuField]-annotated property in [properties] is a primary constructor
     * parameter of [classDeclaration], reporting a compiler error otherwise.
     *
     * Required by `fromByteArray()`, which constructs instances via a named-argument constructor call.
     *
     * @param classDeclaration the struct's class declaration
     * @param properties the struct's [GpuField]-annotated properties
     */
    fun validateConstructorFields(classDeclaration: KSClassDeclaration, properties: List<KSPropertyDeclaration>) {
        val constructorParamNames = classDeclaration.primaryConstructor
            ?.parameters
            ?.mapNotNull { it.name?.asString() }
            ?.toSet()
            ?: emptySet()
        properties.forEach { property ->
            if (property.simpleName.asString() !in constructorParamNames) {
                logger.error(
                    "@GpuField '${property.simpleName.asString()}' must be a primary constructor parameter",
                    property,
                )
            }
        }
    }

    private fun descriptorFor(property: KSPropertyDeclaration): GpuTypeDescriptor {
        val type = property.type.resolve()
        val qualifiedName = type.declaration.qualifiedName?.asString()
        val fixedCount = fixedSizeAnnotationOf(property)
        return when {
            qualifiedName in setOf(
                Int::class.qualifiedName,
                Float::class.qualifiedName,
                Boolean::class.qualifiedName,
            ) ->
                ScalarDescriptor
            qualifiedName in primitiveArrayTypes ->
                PrimitiveArrayDescriptor(fixedCount)
            qualifiedName == genericArrayType -> structArrayDescriptorFor(property, type, fixedCount)
            else -> nestedStructDescriptorFor(property, type)
        }
    }

    private fun structArrayDescriptorFor(
        property: KSPropertyDeclaration,
        type: KSType,
        fixedCount: Int?,
    ): GpuTypeDescriptor {
        val elemType = type.arguments.firstOrNull()
            ?.type
            ?.resolve()
        val elemDecl = elemType?.declaration as? KSClassDeclaration
        if (elemDecl == null || !isGpuStruct(elemDecl)) {
            logger.error("Unsupported array element type: ${property.type}", property)
            return ScalarDescriptor
        }
        if (hasDynamicArrayField(elemDecl)) {
            logger.error("Array element '${elemDecl.simpleName.asString()}' has a dynamic array field", property)
        }
        val alignment = alignAnnotationOf(elemDecl)
            ?: run {
                logger.error(
                    "@GpuStruct array element without @Align: ${elemDecl.simpleName.asString()}",
                    property,
                )
                4
            }
        return StructArrayDescriptor(
            elemDecl,
            alignment,
            ::structSizeOf,
            { isLayoutDependent(gpuFields(it)) },
            fixedCount,
        )
    }

    private fun nestedStructDescriptorFor(property: KSPropertyDeclaration, type: KSType): GpuTypeDescriptor {
        val decl = type.declaration as? KSClassDeclaration
        if (decl == null || !isGpuStruct(decl)) {
            logger.error("Unsupported type: ${property.type}", property)
            return ScalarDescriptor
        }
        if (hasDynamicArrayField(decl)) {
            logger.error("Nested struct '${decl.simpleName.asString()}' has a dynamic array field", property)
        }
        val alignment = alignAnnotationOf(decl)
            ?: run {
                logger.error("@GpuStruct field without @Align: ${decl.simpleName.asString()}", property)
                4
            }
        return GpuStructDescriptor(decl, alignment, ::structSizeOf) {
            isLayoutDependent(gpuFields(it))
        }
    }

    /**
     * Returns true if [declaration] has a dynamically sized (i.e. not [FixedSize]) array field.
     *
     * A struct in that shape has no statically known total size, so it cannot be embedded inside
     * another struct — neither as a plain field nor as an array element (see [descriptorFor]).
     *
     * @param declaration the struct's class declaration to check
     * @return true if the struct has a dynamically sized array field
     */
    private fun hasDynamicArrayField(declaration: KSClassDeclaration): Boolean =
        gpuFields(declaration).any { descriptorFor(it).isDynamic() }

    private fun alignAnnotationOf(declaration: KSClassDeclaration): Int? {
        val annotation = declaration.annotations
            .firstOrNull { it.shortName.asString() == Align::class.simpleName!! }
        return annotation?.arguments?.first()?.value as? Int
    }

    private fun fixedSizeAnnotationOf(property: KSPropertyDeclaration): Int? {
        val annotation = property.annotations
            .firstOrNull { it.shortName.asString() == FixedSize::class.simpleName!! }
        return annotation?.arguments?.first()?.value as? Int
    }

    /**
     * @property property the property declaration
     * @property offset the offset in bytes from the start of the struct
     * @property size the size in bytes of the field; 0 for a dynamically sized array field
     * @property padding the number of bytes of padding before the field
     * @property descriptor the type descriptor resolving alignment and size for this field
     * @property elementSize the size in bytes of a single array element; 0 for non-array fields
     * @property elementStride stride per array element in bytes; 0 for non-array fields
     */
    data class FieldLayout(
        val property: KSPropertyDeclaration,
        val offset: Int,
        val size: Int,
        val padding: Int,
        val descriptor: GpuTypeDescriptor,
        val elementSize: Int = 0,
        val elementStride: Int = 0,
    ) {
        /**
         * Returns true when this field is an array.
         *
         * @return true if this is an array field
         */
        fun isArray(): Boolean = elementStride > 0

        /**
         * Returns true when this field is an array whose element count is only known at runtime.
         *
         * @return true if this is a dynamically sized array field
         */
        fun isDynamicArray(): Boolean = isArray() && descriptor.isDynamic()

        /**
         * Returns the declared element count for a [de.hauschild.kompute.serialization.annotation.FixedSize]
         * array field, or null for a non-array or dynamically sized array field.
         *
         * @return the fixed element count, or null
         */
        fun fixedElementCount(): Int? = if (isArray() && !isDynamicArray()) size / elementStride else null

        /**
         * Returns true when this scalar field is itself a [GpuStruct].
         *
         * @return true if this field's type is a GpuStruct
         */
        fun isGpuStructField(): Boolean = descriptor.isGpuStruct()

        /**
         * Returns true when array elements are themselves [GpuStruct]s.
         *
         * @return true if array elements are GpuStructs
         */
        fun isElementGpuStruct(): Boolean = descriptor.isElementGpuStruct()

        /**
         * Returns the padding bytes written after each array element to fill out the stride.
         *
         * @return inter-element padding in bytes
         */
        fun interElementPadding(): Int = elementStride - elementSize
    }
}
