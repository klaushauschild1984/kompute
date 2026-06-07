package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.requireConfiguration
import kotlin.reflect.KClass

/**
 * A named uniform that passes a single read-only value by name from the CPU to the compute shader.
 *
 * Unlike [StorageBuffer] and [UniformBufferObject], named uniforms are identified by name rather than
 * binding index and do not require a `layout(binding = ...)` declaration in the shader source.
 *
 * Supported types and their GLSL equivalents:
 *
 * Scalars:
 * - [Int] → `int` (or `uint` when [asUnsigned] is set)
 * - [Float] → `float`
 * - [Double] → `double`
 * - [Boolean] → `bool`
 *
 * Vectors (use [FloatArray], [IntArray] or [DoubleArray] with size 2–4):
 * - [FloatArray] → `vec2` / `vec3` / `vec4`
 * - [IntArray] → `ivec2` / `ivec3` / `ivec4` (or `uvec*` when [asUnsigned] is set)
 * - [DoubleArray] → `dvec2` / `dvec3` / `dvec4`
 *
 * Matrices (use [FloatArray] or [DoubleArray] with [asMatrix]):
 * - [FloatArray] + [asMatrix] → `mat2` / `mat3` / `mat4` / `mat{C}x{R}`
 * - [DoubleArray] + [asMatrix] → `dmat2` / `dmat3` / `dmat4` / `dmat{C}x{R}`
 *
 * @param T the value type — must be one of the supported types listed above
 * @property name the uniform name as declared in the shader source — must not be blank
 * @property type the [KClass] of [T], used to determine the correct GL call
 */
class NamedUniform<T: Any>(
    override val name: String,
    val type: KClass<T>
) : ShaderData, NamedBinding {
    /**
     * Input data to upload to the GPU.
     */
    var value: T? = null
        private set

    /**
     * Whether this uniform should be passed as an unsigned integer. Only valid for [Int] and [IntArray] uniforms.
     */
    var unsigned: Boolean = false
        private set

    /**
     * Whether this uniform is a matrix. Holds the number of rows and columns.
     */
    var matrixDimension: MatrixDimension? = null
        private set

    /**
     * Sets the input value.
     *
     * @param value the value to upload to the GPU
     * @return this [NamedUniform] for chaining
     */
    fun value(value: T): NamedUniform<T> {
        this.value = value
        return this
    }

    /**
     * Marks this uniform as unsigned. Only valid for [Int] and [IntArray] uniforms — validated in [validate].
     *
     * @return this [NamedUniform] for chaining
     */
    fun asUnsigned(): NamedUniform<T> {
        this.unsigned = true
        return this
    }

    /**
     * Marks this uniform as a matrix and specifies the number of rows and columns. Allowed values are {2, 3, 4}
     *
     * Only compatible with type[FloatArray].
     *
     * @param rows
     * @param columns
     * @return this [NamedUniform] for chaining
     */
    fun asMatrix(rows: Int, columns: Int): NamedUniform<T> {
        this.matrixDimension = MatrixDimension(rows, columns)
        return this
    }

    /**
     * Convenience methods for `asMatrix(2, 2)`.
     */
    fun as2By2Matrix(): NamedUniform<T> = asMatrix(2, 2)
    /**
     * Convenience methods for `asMatrix(3, 3)`.
     */
    fun as3By3Matrix(): NamedUniform<T> = asMatrix(3, 3)
    /**
     * Convenience methods for `asMatrix(3, 3)`.
     */
    fun as4By4Matrix(): NamedUniform<T> = asMatrix(4, 4)

    /**
     * Validates the uniform configuration.
     *
     * @throws [de.hauschild.kompute.core.exception.KomputeConfigurationException] if the name is blank, the type is
     * unsupported, [asUnsigned] is set on a non-[Int] type, or no value has been provided
     */
    override fun validate() {
        super.validate()
        requireConfiguration(type in SUPPORTED_TYPES) {
            "Unsupported NamedUniform type: ${type.simpleName}"
        }
        if (unsigned) {
            requireConfiguration(type == Int::class || type == IntArray::class) {
                "Unsigned NamedUniforms must be of type Int or IntArray"
            }
            requireConfiguration(matrixDimension == null) {
                "Matrix uniforms cannot be unsigned"
            }
        }
        requireConfiguration(value != null) {
            "A value must be provided"
        }
        validateMatrix()
    }

    private fun validateMatrix() {
        matrixDimension?.let { dim ->
            requireConfiguration(type == FloatArray::class || type == DoubleArray::class) {
                "Matrix uniforms only support FloatArray and DoubleArray"
            }
            val validDimensions = setOf(2, 3, 4)
            requireConfiguration(dim.rows in validDimensions && dim.columns in validDimensions) {
                "Matrix dimensions must each be 2, 3, or 4"
            }
            val expectedSize = dim.rows * dim.columns
            val actualSize = when (val v = value) {
                is FloatArray -> v.size
                is DoubleArray -> v.size
                else -> null
            }
            requireConfiguration(actualSize == expectedSize) {
                "Value size must be $expectedSize for a ${dim.rows}×${dim.columns} matrix"
            }
        }

        if (matrixDimension == null && type in SUPPORTED_ARRAY_TYPES) {
            val actualSize = when (val v = value) {
                is FloatArray -> v.size
                is IntArray -> v.size
                is DoubleArray -> v.size
                else -> null
            }
            requireConfiguration(actualSize in setOf(2, 3, 4)) {
                "Vector uniform size must be 2, 3, or 4"
            }
        }
    }

    override fun toString(): String {
        val valueInfo = when (val v = value) {
            is FloatArray  -> matrixDimension?.let { "Matrix(${it.rows}×${it.columns})" } ?: "Vector(${v.size})"
            is IntArray    -> "Vector(${v.size})"
            is DoubleArray -> matrixDimension?.let { "Matrix(${it.rows}×${it.columns})" } ?: "Vector(${v.size})"
            null           -> "no value"
            else           -> "$v"
        }
        return "NamedUniform<${type.simpleName}>(name=$name)" +
            (if (unsigned) "(unsigned)" else "") +
            "($valueInfo)"
    }

    /**
     * @property rows
     * @property columns
     */
    data class MatrixDimension(val rows: Int, val columns: Int)
    companion object {
        private val SUPPORTED_ARRAY_TYPES : Set<KClass<*>> =
            setOf(
                IntArray::class,
                FloatArray::class,
                DoubleArray::class,
            )
        private val SUPPORTED_TYPES : Set<KClass<*>> =
            setOf(
                Int::class,
                Float::class,
                Double::class,
                Boolean::class,
            ) + SUPPORTED_ARRAY_TYPES

        /**
         * Creates a [NamedUniform] with a reified type parameter for idiomatic Kotlin usage.
         *
         * @param name the binding name in the shader — must be not blank
         * @return a new [NamedUniform] with the inferred type [T]
         */
        inline operator fun <reified T : Any> invoke(name: String) = NamedUniform(name, T::class)

        /**
         * Convenience method to create a [NamedUniform] from Java using the [Class] type.
         *
         * @param name the binding name in the shader
         * @param type the Java [Class] of the buffer data type
         * @return a new [NamedUniform] with the given name and type
         */
        @JvmStatic
        fun <T : Any> newNamedUniform(
            name: String,
            type: Class<T>,
        ) = NamedUniform(name, type.kotlin)
    }
}
