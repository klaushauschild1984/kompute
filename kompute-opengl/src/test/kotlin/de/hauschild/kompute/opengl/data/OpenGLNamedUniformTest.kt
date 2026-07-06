package de.hauschild.kompute.opengl.data

import de.hauschild.kompute.core.data.NamedUniform
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.shader.ShaderSource.Code
import de.hauschild.kompute.opengl.OpenGLBackendExtension
import de.hauschild.kompute.opengl.backend.OpenGLBackend
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLNamedUniformTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource
    fun `named uniform`(
        glslType: String,
        value: Any,
        unsigned: Boolean,
        namedUniformType: KClass<*>,
        backend: OpenGLBackend
    ) {
        @Suppress("UNCHECKED_CAST")
        var uniform = NamedUniform.newNamedUniform("value", namedUniformType.java as Class<Any>).value(value)
        if (unsigned) {
            uniform = uniform.asUnsigned()
        }
        val output = StorageBuffer.newStorageBuffer(0, scalarStorageBufferType(namedUniformType)).size(1).asOutput()

        val result = backend
            .shader(Code(scalarUniformSource(glslType)))
            .compile()
            .use { compiledShader ->
                compiledShader.dispatch(1, uniform, output)
                    .use { it[output] }
            }

        when (result) {
            is IntArray -> assertThat(result).containsExactly(value as Int)
            is FloatArray -> assertThat(result).containsExactly(value as Float)
            is DoubleArray -> assertThat(result).containsExactly(value as Double)
            else -> error("Unexpected result type: ${result::class}")
        }
    }

    @Test
    fun `boolean named uniform`(backend: OpenGLBackend) {
        val uniform = NamedUniform<Boolean>("value").value(true)
        val output = StorageBuffer<IntArray>(0).size(1).asOutput()

        val result = backend
            .shader(Code(BOOLEAN_UNIFORM_SOURCE))
            .compile()
            .use { compiledShader ->
                compiledShader.dispatch(1, uniform, output)
                    .use { it[output] }
            }

        assertThat(result).containsExactly(1)
    }

    @ParameterizedTest(name = "{0}[{2}]")
    @MethodSource
    fun `vector named uniform`(
        glslType: String,
        arrayType: KClass<*>,
        size: Int,
        value: Any,
        backend: OpenGLBackend
    ) {
        @Suppress("UNCHECKED_CAST")
        val uniform = NamedUniform.newNamedUniform("value", arrayType.java as Class<Any>).value(value)
        val output = StorageBuffer.newStorageBuffer(0, arrayType.java).size(size).asOutput()

        val result = backend
            .shader(Code(vectorUniformSource(glslType, size)))
            .compile()
            .use { compiledShader ->
                compiledShader.dispatch(1, uniform, output)
                    .use { it[output] }
            }

        when (result) {
            is IntArray -> assertThat(result).containsExactly(*(value as IntArray))
            is FloatArray -> assertThat(result).containsExactly(*(value as FloatArray))
            is DoubleArray -> assertThat(result).containsExactly(*(value as DoubleArray))
            else -> error("Unexpected result type: ${result::class}")
        }
    }

    @Test
    fun `2 by 2 matrix named uniform`(backend: OpenGLBackend) {
        val matrix = floatArrayOf(1f, 2f, 3f, 4f)
        val uniform = NamedUniform<FloatArray>("value").value(matrix).as2By2Matrix()
        val output = StorageBuffer<FloatArray>(0).size(4).asOutput()

        val result = backend
            .shader(Code(MATRIX_2X2_UNIFORM_SOURCE))
            .compile()
            .use { compiledShader ->
                compiledShader.dispatch(1, uniform, output)
                    .use { it[output] }
            }

        assertThat(result).containsExactly(*matrix)
    }

    private fun scalarUniformSource(glslType: String) = """
        #version 430 core
        layout(local_size_x = 1) in;

        layout(std430, binding = 0) writeonly buffer Output {
            $glslType values[];
        } result;

        uniform $glslType value;

        void main() {
            result.values[0] = value;
        }
    """.trimIndent()

    private fun vectorUniformSource(
        glslType: String,
        size: Int,
    ) = """
        #version 430 core
        layout(local_size_x = 1) in;

        layout(std430, binding = 0) writeonly buffer Output {
            $glslType values[];
        } result;

        uniform ${vectorGlslType(glslType, size)} value;

        void main() {
            ${(0 until size).joinToString("\n            ") { "result.values[$it] = value[$it];" }}
        }
    """.trimIndent()

    private fun vectorGlslType(
        glslType: String,
        size: Int,
    ) = when (glslType) {
        "int" -> "ivec$size"
        "float" -> "vec$size"
        "double" -> "dvec$size"
        else -> error("Unsupported vector element type: $glslType")
    }

    private fun scalarStorageBufferType(namedUniformType: KClass<*>) = when (namedUniformType) {
        Int::class -> IntArray::class.java
        Float::class -> FloatArray::class.java
        Double::class -> DoubleArray::class.java
        else -> error("Unsupported scalar uniform type: $namedUniformType")
    }

    companion object {
        private val BOOLEAN_UNIFORM_SOURCE = """
            #version 430 core
            layout(local_size_x = 1) in;

            layout(std430, binding = 0) writeonly buffer Output {
                int values[];
            } result;

            uniform bool value;

            void main() {
                result.values[0] = value ? 1 : 0;
            }
        """.trimIndent()
        private val MATRIX_2X2_UNIFORM_SOURCE = """
            #version 430 core
            layout(local_size_x = 1) in;

            layout(std430, binding = 0) writeonly buffer Output {
                float values[];
            } result;

            uniform mat2 value;

            void main() {
                result.values[0] = value[0][0];
                result.values[1] = value[0][1];
                result.values[2] = value[1][0];
                result.values[3] = value[1][1];
            }
        """.trimIndent()

        @JvmStatic
        fun `named uniform`() = listOf(
            Arguments.of("int", 42, false, Int::class),
            Arguments.of("uint", 42, true, Int::class),
            Arguments.of("float", 3.14f, false, Float::class),
            Arguments.of("double", 2.71, false, Double::class),
        )

        @JvmStatic
        fun `vector named uniform`() = listOf(
            Arguments.of("int", IntArray::class, 3, intArrayOf(1, 2, 3)),
            Arguments.of("float", FloatArray::class, 3, floatArrayOf(1f, 2f, 3f)),
            Arguments.of("double", DoubleArray::class, 2, doubleArrayOf(1.0, 2.0)),
        )
    }
}
