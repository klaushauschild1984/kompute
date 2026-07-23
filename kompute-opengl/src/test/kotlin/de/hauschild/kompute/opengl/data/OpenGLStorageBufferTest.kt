package de.hauschild.kompute.opengl.data

import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.shader.ShaderSource.Glsl.Code
import de.hauschild.kompute.opengl.OpenGLBackendExtension
import de.hauschild.kompute.opengl.backend.OpenGLBackend
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.reflect.KClass

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLStorageBufferTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource
    fun `storage buffer`(
        glslType: String,
        input: Any,
        type: KClass<*>,
        backend: OpenGLBackend
    ) {
        @Suppress("UNCHECKED_CAST")
        val inputBuffer = StorageBuffer.newStorageBuffer(0, type.java as Class<Any>).data(input)
        val outputBuffer = StorageBuffer.newStorageBuffer(1, type.java).size(3).asOutput()

        val result = backend
            .shader(
                Code(storageBufferSource(glslType)),
            )
            .compile()
            .use { compiledShader ->
                compiledShader.dispatch(3, inputBuffer, outputBuffer)
                    .use { it[outputBuffer] }
            }

        when (result) {
            is IntArray -> assertThat(result).containsExactly(*(input as IntArray))
            is LongArray -> assertThat(result).containsExactly(*(input as LongArray))
            is FloatArray -> assertThat(result).containsExactly(*(input as FloatArray))
            is DoubleArray -> assertThat(result).containsExactly(*(input as DoubleArray))
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    fun `read-write storage buffer`(
        glslType: String,
        input: Any,
        type: KClass<*>,
        backend: OpenGLBackend
    ) {
        @Suppress("UNCHECKED_CAST")
        val buffer = StorageBuffer.newStorageBuffer(0, type.java as Class<Any>)
            .data(input)
            .asOutput()

        val result = backend
            .shader(Code(readWriteStorageBufferSource(glslType)))
            .compile()
            .use { compiledShader ->
                compiledShader.dispatch(3, buffer)
                    .use { it[buffer] }
            }

        when (result) {
            is IntArray -> assertThat(result).containsExactly(*(input as IntArray))
            is LongArray -> assertThat(result).containsExactly(*(input as LongArray))
            is FloatArray -> assertThat(result).containsExactly(*(input as FloatArray))
            is DoubleArray -> assertThat(result).containsExactly(*(input as DoubleArray))
        }
    }

    @Test
    fun `handle reuse`() {
        val storageBuffer = StorageBuffer<FloatArray>(0).data(floatArrayOf(1.0f, 2.0f, 3.0f)).asOutput()

        val first = OpenGLStorageBuffer(storageBuffer)
        first.bind()
        val handle = first.glHandle

        val second = OpenGLStorageBuffer(storageBuffer)
        second.bind()
        assertThat(second.glHandle).isEqualTo(handle)

        second.close()
    }

    companion object {
        private fun storageBufferSource(glslType: String): String {
            val extension = if (glslType == "int64_t") "\n#extension GL_ARB_gpu_shader_int64 : require" else ""
            return """
                    #version 430 core$extension

                    layout (local_size_x = 1) in;

                    layout (std430, binding = 0) readonly buffer InputBuffer {
                        $glslType values[];
                    } source;

                    layout (std430, binding = 1) writeonly buffer OutputBuffer {
                        $glslType values[];
                    } result;

                    void main() {
                        result.values[gl_GlobalInvocationID.x] = source.values[gl_GlobalInvocationID.x];
                    }
                """.trimIndent()
        }

        private fun readWriteStorageBufferSource(glslType: String): String {
            val extension = if (glslType == "int64_t") "\n#extension GL_ARB_gpu_shader_int64 : require" else ""
            return """
                    #version 430 core$extension

                    layout (local_size_x = 1) in;

                    layout (std430, binding = 0) buffer Buffer {
                        $glslType values[];
                    } data;

                    void main() {
                        data.values[gl_GlobalInvocationID.x] = data.values[gl_GlobalInvocationID.x];
                    }
                """.trimIndent()
        }

        @JvmStatic
        fun `storage buffer`(): Stream<Arguments> = Stream.of(
            Arguments.of("int", intArrayOf(1, 2, 3), IntArray::class),
            Arguments.of("int64_t", longArrayOf(1, 2, 3), LongArray::class),
            Arguments.of("float", floatArrayOf(1f, 2f, 3f), FloatArray::class),
            Arguments.of("double", doubleArrayOf(1.0, 2.0, 3.0), DoubleArray::class),
        )

        @JvmStatic
        fun `read-write storage buffer`(): Stream<Arguments> = Stream.of(
            Arguments.of("int", intArrayOf(1, 2, 3), IntArray::class),
            Arguments.of("int64_t", longArrayOf(1, 2, 3), LongArray::class),
            Arguments.of("float", floatArrayOf(1f, 2f, 3f), FloatArray::class),
            Arguments.of("double", doubleArrayOf(1.0, 2.0, 3.0), DoubleArray::class),
        )
    }
}
