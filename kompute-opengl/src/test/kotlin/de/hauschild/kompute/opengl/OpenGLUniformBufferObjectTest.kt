package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.data.UniformBufferObject
import de.hauschild.kompute.core.execution.ShaderSource.Code
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.extension.ExtendWith

import java.nio.ByteBuffer
import java.nio.ByteOrder

import kotlin.test.Test

@ExtendWith(OpenGLBackendExtension::class)
class OpenGLUniformBufferObjectTest {
    @Test
    fun `float array`(backend: OpenGLBackend) {
        val output = StorageBuffer<FloatArray>(1).size(3).asOutput()
        val result =
            backend
                .shader(
                    Code("""
#version 430 core
layout (local_size_x = 1) in;

layout (std140, binding = 0) uniform Params {
    float value;
} params;

layout (std430, binding = 1) writeonly buffer OutputBuffer {
    float values[];
} outputBuffer;

void main() {
    outputBuffer.values[gl_GlobalInvocationID.x] = params.value;
}
                    """.trimIndent()),
                )
                .compile()
                .use { compiledShader ->
                    compiledShader.dispatch(3,
                        UniformBufferObject(0).data(
                            ByteBuffer
                                .allocate(Float.SIZE_BYTES)
                                .order(ByteOrder.nativeOrder())
                                .putFloat(42f)
                                .array()
                        ),
                        output,
                    )
                }[output]

        assertArrayEquals(floatArrayOf(42f, 42f, 42f), result)
    }
}
