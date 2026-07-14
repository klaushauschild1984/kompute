/**
 * Type descriptors for GPU field types, providing alignment and size information
 * for [GpuStructLayout] memory layout computation.
 */

package de.hauschild.kompute.serialization.processor

import de.hauschild.kompute.serialization.annotation.Layout
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Provides alignment, size, and element stride for a single GPU field type.
 *
 * Implementations are created by `GpuStructLayout.descriptorFor` and stored in
 * [GpuStructLayout.FieldLayout.descriptor].
 */
interface GpuTypeDescriptor {
    /**
     * Returns true when this type is a GPU array with a dynamic element count.
     *
     * @return true if this is an array type
     */
    fun isArray(): Boolean = false

    /**
     * Returns true when array elements are themselves [de.hauschild.kompute.serialization.annotation.GpuStruct]s.
     *
     * @return true if array elements are GpuStructs
     */
    fun isElementGpuStruct(): Boolean = false

    /**
     * Returns true when this scalar field is itself a [de.hauschild.kompute.serialization.annotation.GpuStruct].
     *
     * @return true if this type is a GpuStruct
     */
    fun isGpuStruct(): Boolean = false

    /**
     * Returns the alignment requirement for this type under [layout].
     *
     * @param layout the memory layout standard to apply
     * @return alignment in bytes
     */
    fun alignment(layout: Layout): Int

    /**
     * Returns the size in bytes; 0 for array fields whose total size is dynamic.
     *
     * @param layout the memory layout standard to apply
     * @return size in bytes, or 0 for dynamic array fields
     */
    fun size(layout: Layout): Int

    /**
     * Returns the size in bytes of a single array element; 0 for non-array types.
     *
     * @param layout the memory layout standard to apply
     * @return element size in bytes, or 0 for non-array types
     */
    fun elementSize(layout: Layout): Int = 0

    /**
     * Returns the stride in bytes per array element (element size + inter-element padding); 0 for non-array types.
     *
     * @param layout the memory layout standard to apply
     * @return stride in bytes, or 0 for non-array types
     */
    fun elementStride(layout: Layout): Int = 0

    /**
     * Specifies whether this type is layout-dependent, i.e. depends on the GPU memory layout standard.
     *
     * @return 'true' if this type is layout-dependent, 'false' otherwise
     */
    fun isLayoutDependent(): Boolean = false
}

/**
 * Descriptor for scalar GPU types: [Int], [Float], [Boolean].
 */
object ScalarDescriptor : GpuTypeDescriptor {
    override fun alignment(layout: Layout) = 4
    override fun size(layout: Layout) = 4
}

/**
 * Descriptor for a nested [de.hauschild.kompute.serialization.annotation.GpuStruct] field.
 *
 * @param declaredAlignment alignment in bytes from its [de.hauschild.kompute.serialization.annotation.Align]
 * @param computeStructSize computes the serialized size of [declaration] for a given layout
 * @param checkLayoutDependent checks if the struct is layout-dependent
 * @property declaration the nested struct's class declaration
 */
class GpuStructDescriptor(
    val declaration: KSClassDeclaration,
    private val declaredAlignment: Int,
    private val computeStructSize: (KSClassDeclaration, Layout) -> Int,
    private val checkLayoutDependent: (KSClassDeclaration) -> Boolean,
) : GpuTypeDescriptor {
    override fun isGpuStruct() = true
    override fun alignment(layout: Layout) = declaredAlignment
    override fun size(layout: Layout) = computeStructSize(declaration, layout)
    override fun isLayoutDependent() = checkLayoutDependent(declaration)
}

/**
 * Descriptor for primitive GPU array types: [FloatArray], [IntArray], [BooleanArray].
 */
object PrimitiveArrayDescriptor : GpuTypeDescriptor {
    override fun isArray() = true
    override fun alignment(layout: Layout) = if (layout == Layout.STD140) 16 else 4
    override fun size(layout: Layout) = 0
    override fun elementSize(layout: Layout) = 4
    override fun elementStride(layout: Layout) = alignment(layout)
    override fun isLayoutDependent() = true
}

/**
 * Descriptor for [Array] fields whose element type is a [de.hauschild.kompute.serialization.annotation.GpuStruct].
 *
 * @param elementDeclaredAlignment alignment in bytes, from [de.hauschild.kompute.serialization.annotation.Align]
 * @param computeStructSize computes the serialized size of the element declaration for a given layout
 * @param checkLayoutDependent
 * @property elementDeclaration the element struct's class declaration
 */
class StructArrayDescriptor(
    val elementDeclaration: KSClassDeclaration,
    private val elementDeclaredAlignment: Int,
    private val computeStructSize: (KSClassDeclaration, Layout) -> Int,
    private val checkLayoutDependent: (KSClassDeclaration) -> Boolean,
) : GpuTypeDescriptor {
    override fun isArray() = true
    override fun isElementGpuStruct() = true
    override fun alignment(layout: Layout) =
        if (layout == Layout.STD140) maxOf(elementDeclaredAlignment, 16) else elementDeclaredAlignment
    override fun size(layout: Layout) = 0
    override fun elementSize(layout: Layout) = computeStructSize(elementDeclaration, layout)
    override fun elementStride(layout: Layout): Int {
        val elemSize = elementSize(layout)
        val align = alignment(layout)
        val remainder = elemSize % align
        return if (remainder == 0) elemSize else elemSize + (align - remainder)
    }
    override fun isLayoutDependent() =
        elementDeclaredAlignment < 16 || checkLayoutDependent(elementDeclaration)
}
