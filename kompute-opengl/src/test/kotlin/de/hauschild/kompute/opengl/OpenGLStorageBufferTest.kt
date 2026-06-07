package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.execution.ShaderSource.Code
import org.junit.jupiter.api.Assertions.assertArrayEquals
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

        val glslSource = """
#version 430 core
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
                    """

        val result = backend
            .shader(
                Code(
                    glslSource.trimIndent()
                ),
            )
            .data(
                inputBuffer,
                outputBuffer,
            )
            .dispatch(3)
            .execute()

        when (val result = result[outputBuffer]) {
            is FloatArray -> assertArrayEquals(input as FloatArray, result)
            is IntArray -> assertArrayEquals(input as IntArray, result)
            is DoubleArray -> assertArrayEquals(input as DoubleArray, result)
        }
    }

    companion object {
        @JvmStatic
        fun `storage buffer`(): Stream<Arguments> = Stream.of(
            Arguments.of("int", intArrayOf(1, 2, 3), IntArray::class),
            Arguments.of("float", floatArrayOf(1f, 2f, 3f), FloatArray::class),
            Arguments.of("double", doubleArrayOf(1.0, 2.0, 3.0), DoubleArray::class),
        )
    }
}
