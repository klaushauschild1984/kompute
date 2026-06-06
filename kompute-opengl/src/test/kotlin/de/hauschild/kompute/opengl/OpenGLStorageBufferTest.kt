package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.execution.ShaderSource.Code
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.Test

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLStorageBufferTest {
    @Test
    fun `float array`(backend: OpenGLBackend) {
        val output = StorageBuffer<FloatArray>(1).size(3).asOutput()
        val result =
            backend
                .shader(
                    Code("""
#version 430 core
layout (local_size_x = 1) in;

layout (std430, binding = 0) readonly buffer InputBuffer {
    float values[];
} source;

layout (std430, binding = 1) writeonly buffer OutputBuffer {
    float values[];
} result;

void main() {
    result.values[gl_GlobalInvocationID.x] = source.values[gl_GlobalInvocationID.x];
}
                    """.trimIndent()),
                )
                .data(
                    StorageBuffer<FloatArray>(0).data(floatArrayOf(1f, 2f, 3f)),
                    output,
                )
                .dispatch(3)
                .execute()[output]

        assertArrayEquals(floatArrayOf(1f, 2f, 3f), result)
    }
}
