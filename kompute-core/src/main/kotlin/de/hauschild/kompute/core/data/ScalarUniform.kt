package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.requireConfiguration
import kotlin.reflect.KClass

/**
 * A scalar uniform that passes a single read-only value by name from the CPU to the compute shader.
 *
 * Unlike [StorageBuffer] and [UniformBuffer], scalar uniforms are identified by name rather than
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
class ScalarUniform<T: Any>(
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
     * Sets the input value.
     *
     * @param value the value to upload to the GPU
     * @return this [ScalarUniform] for chaining
     */
    fun value(value: T): ScalarUniform<T> {
        this.value = value
        return this
    }

    /**
     * Marks this uniform as unsigned. Only valid for [Int] uniforms — validated in [validate].
     *
     * @return this [ScalarUniform] for chaining
     */
    fun unsigned(): ScalarUniform<T> {
        this.unsigned = true
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
            "Unsupported ScalarUniform type: ${type.simpleName}"
        }
        if (unsigned) {
            requireConfiguration(type == Int::class) {
                "Unsigned ScalarUniforms must be of type Int"
            }
        }
        requireConfiguration(value != null) {
            "A value must be provided"
        }
    }
    override fun toString(): String = "ScalarUniform<${type.simpleName}>(name=$name)"
    companion object {
        private val SUPPORTED_TYPES =
            setOf(
                Int::class,
                Float::class,
                Double::class,
                Boolean::class,
            )

        /**
         * Creates a [ScalarUniform] with a reified type parameter for idiomatic Kotlin usage.
         *
         * @param name the binding name in the shader — must be not blank
         * @return a new [ScalarUniform] with the inferred type [T]
         */
        inline operator fun <reified T : Any> invoke(name: String) = ScalarUniform(name, T::class)

        /**
         * Convenience method to create a [ScalarUniform] from Java using the [Class] type.
         *
         * @param name the binding name in the shader
         * @param type the Java [Class] of the buffer data type
         * @return a new [ScalarUniform] with the given name and type
         */
        @JvmStatic
        fun <T : Any> newScalarUniform(
            name: String,
            type: Class<T>,
        ) = ScalarUniform(name, type.kotlin)
    }
}
