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
 * - [Int] → `int` (or `uint` when [unsigned] is set)
 * - [Float] → `float`
 * - [Double] → `double`
 * - [Boolean] → `bool`
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
     * Whether this uniform should be passed as an unsigned integer. Only valid for [Int] uniforms.
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
     * Marks this uniform as unsigned. Only valid for [Int] uniforms — validated in [validate].
     *
     * @return this [NamedUniform] for chaining
     */
    fun unsigned(): NamedUniform<T> {
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
     * @return
     */
    fun asMatrix(rows: Int, columns: Int): NamedUniform<T> {
        this.matrixDimension = MatrixDimension(rows, columns)
        return this
    }

    /**
     * Validates the uniform configuration.
     *
     * @throws [de.hauschild.kompute.core.exception.KomputeConfigurationException] if the name is blank, the type is
     * unsupported, [unsigned] is set on a non-[Int] type, or no value has been provided
     */
    override fun validate() {
        super.validate()
        requireConfiguration(type in SUPPORTED_TYPES) {
            "Unsupported NamedUniform type: ${type.simpleName}"
        }
        if (unsigned) {
            requireConfiguration(type == Int::class) {
                "Unsigned NamedUniforms must be of type Int"
            }
            requireConfiguration(matrixDimension == null) {
                "Matrix uniforms cannot be unsigned"
            }
        }
        requireConfiguration(value != null) {
            "A value must be provided"
        }
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

        if (matrixDimension == null && (type == FloatArray::class || type == IntArray::class ||
                type == DoubleArray::class)) {
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
    override fun toString(): String = "NamedUniform<${type.simpleName}>(name=$name)" +
            (if (unsigned) "(unsigned)" else "") +
            "${matrixDimension?.let { "(as ${it.rows}x${it.columns} matrix)" } ?: ""}}"

    /**
     * @property rows
     * @property columns
     */
    data class MatrixDimension(val rows: Int, val columns: Int)
    companion object {
        private val SUPPORTED_TYPES : Set<KClass<*>> =
            setOf(
                Int::class,
                Float::class,
                Double::class,
                Boolean::class,
                IntArray::class,
                FloatArray::class,
                DoubleArray::class,
            )

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
