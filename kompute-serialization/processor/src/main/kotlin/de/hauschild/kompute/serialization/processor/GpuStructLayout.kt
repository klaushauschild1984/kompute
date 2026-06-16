package de.hauschild.kompute.serialization.processor

import de.hauschild.kompute.serialization.annotation.Align
import de.hauschild.kompute.serialization.annotation.GpuField
import de.hauschild.kompute.serialization.annotation.GpuStruct
import de.hauschild.kompute.serialization.annotation.Layout
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Computes the memory layout of [GpuStruct] annotated classes according to std140 and std430 rules.
 *
 * @param logger used to report unsupported field types as compiler errors
 */
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
     * Array fields must appear last; a compiler error is emitted otherwise.
     * Array fields have [FieldLayout.size] of 0 (dynamic at runtime) and carry [FieldLayout.elementStride].
     *
     * @param properties the [GpuField]-annotated properties to lay out
     * @param layout the memory layout standard to apply
     * @return list of [FieldLayout] entries with computed offsets and padding
     */
    fun computeLayout(properties: List<KSPropertyDeclaration>, layout: Layout): List<FieldLayout> {
        var offset = 0
        return properties.mapIndexed { index, property ->
            val descriptor = descriptorFor(property)
            if (descriptor.isArray() && index != properties.lastIndex) {
                logger.error("Array fields must be the last field in a @GpuStruct", property)
            }
            val alignment = descriptor.alignment(layout)
            val padding = (alignment - (offset % alignment)) % alignment
            offset += padding
            if (descriptor.isArray()) {
                FieldLayout(property, offset, 0, padding, descriptor, descriptor.elementSize(layout),
                    descriptor.elementStride(layout))
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
     * Returns 0 when the last property is an array field, since array size is dynamic.
     *
     * @param rawSize the unpadded size of the struct in bytes
     * @param properties the struct's [GpuField]-annotated properties
     * @param layout the memory layout standard to apply
     * @return number of trailing padding bytes, 0 if already aligned or last field is an array
     */
    fun trailingPadding(
        rawSize: Int,
        properties: List<KSPropertyDeclaration>,
        layout: Layout
    ): Int {
        val descriptors = properties.map { descriptorFor(it) }
        if (descriptors.lastOrNull()?.isArray() == true) {
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

    private fun computeStructSizeOf(declaration: KSClassDeclaration, layout: Layout): Int {
        val innerProperties = gpuFields(declaration)
        val innerLayout = computeLayout(innerProperties, layout)
        val rawSize = innerLayout.lastOrNull()?.let { it.offset + it.size } ?: 0
        return rawSize + trailingPadding(rawSize, innerProperties, layout)
    }

    private fun descriptorFor(property: KSPropertyDeclaration): GpuTypeDescriptor {
        val type = property.type.resolve()
        val qualifiedName = type.declaration.qualifiedName?.asString()
        return when {
            qualifiedName in setOf(
                Int::class.qualifiedName,
                Float::class.qualifiedName,
                Boolean::class.qualifiedName,
            ) ->
                ScalarDescriptor
            qualifiedName in primitiveArrayTypes ->
                PrimitiveArrayDescriptor
            qualifiedName == genericArrayType -> {
                val elemType = type.arguments.firstOrNull()
                    ?.type
                    ?.resolve()
                val elemDecl = elemType?.declaration as? KSClassDeclaration
                if (elemDecl != null && isGpuStruct(elemDecl)) {
                    val alignment = alignAnnotationOf(elemDecl)
                        ?: run {
                            logger.error(
                                "@GpuStruct array element without @Align: ${elemDecl.simpleName.asString()}",
                                property,
                            )
                            4
                        }
                    StructArrayDescriptor(elemDecl, alignment, ::computeStructSizeOf) {
                        isLayoutDependent(gpuFields(it))
                    }
                } else {
                    logger.error("Unsupported array element type: ${property.type}", property)
                    ScalarDescriptor
                }
            }
            else -> {
                val decl = type.declaration as? KSClassDeclaration
                if (decl != null && isGpuStruct(decl)) {
                    val alignment = alignAnnotationOf(decl)
                        ?: run {
                            logger.error(
                                "@GpuStruct field without @Align: ${decl.simpleName.asString()}",
                                property,
                            )
                            4
                        }
                    GpuStructDescriptor(decl, alignment, ::computeStructSizeOf) {
                        isLayoutDependent(gpuFields(it))
                    }
                } else {
                    logger.error("Unsupported type: ${property.type}", property)
                    ScalarDescriptor
                }
            }
        }
    }

    private fun alignAnnotationOf(declaration: KSClassDeclaration): Int? {
        val annotation = declaration.annotations
            .firstOrNull { it.shortName.asString() == Align::class.simpleName!! }
        return annotation?.arguments?.first()?.value as? Int
    }

    /**
     * @property property the property declaration
     * @property offset the offset in bytes from the start of the struct
     * @property size the size in bytes of the field; 0 for array fields (dynamic at runtime)
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
