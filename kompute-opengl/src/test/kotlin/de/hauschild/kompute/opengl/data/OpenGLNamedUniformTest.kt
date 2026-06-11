package de.hauschild.kompute.opengl.data

import de.hauschild.kompute.core.data.NamedUniform
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.shader.ShaderSource.Code
import de.hauschild.kompute.opengl.OpenGLBackendExtension
import de.hauschild.kompute.opengl.backend.OpenGLBackend
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass
import kotlin.test.assertEquals

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLNamedUniformTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource
    fun `named uniform`(
        glslType: String,
        value: Any,
        namedUniformType: KClass<*>,
        storageBufferType: KClass<*>,
        backend: OpenGLBackend
    ) {
        @Suppress("UNCHECKED_CAST")
        val uniform = NamedUniform.newNamedUniform("value", namedUniformType.java as Class<Any>).value(value)
        val output = StorageBuffer.newStorageBuffer(0, storageBufferType.java).size(1).asOutput()

        val result = backend
            .shader(Code(namedUniformSource(glslType)))
            .compile()
            .use { it.dispatch(1, uniform, output) }

        when (val result = result[output]) {
            is Int -> assertEquals(value as Int, result)
            is Float -> assertEquals(value as Float, result)
            is Double -> assertEquals(value as Double, result)
        }
    }

    private fun namedUniformSource(glslType: String) = """
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

    companion object {
        @JvmStatic
        fun `named uniform`() = listOf(
            Arguments.of("int", 42, Int::class, IntArray::class),
            Arguments.of("float", 3.14f, Float::class, FloatArray::class,),
            Arguments.of("double", 2.71, Double::class, DoubleArray::class,),
        )
    }
}
