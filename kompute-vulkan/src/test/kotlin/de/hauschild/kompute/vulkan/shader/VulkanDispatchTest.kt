package de.hauschild.kompute.vulkan.shader

import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.data.UniformBufferObject
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import de.hauschild.kompute.core.shader.ShaderSource.Glsl.Code
import de.hauschild.kompute.vulkan.VulkanBackendExtension
import de.hauschild.kompute.vulkan.backend.VulkanBackend
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

@ExtendWith(VulkanBackendExtension::class)
class VulkanDispatchTest {
    @Test
    fun `storage buffer is processed on the gpu`(backend: VulkanBackend) {
        val input = StorageBuffer<FloatArray>(0).data(FloatArray(100) { it.toFloat() })
        val output = StorageBuffer<FloatArray>(1).size(100).asOutput()

        val result = backend.shader(Code(DOUBLE_SHADER)).compile().use { shader ->
            shader.dispatch(100, input, output).use { it[output] }
        }

        assertThat(result).containsExactly(*FloatArray(100) { it * 2f })
    }

    @Test
    fun `read-write storage buffer is modified in place`(backend: VulkanBackend) {
        val buffer = StorageBuffer<IntArray>(0).data(intArrayOf(1, 2, 3, 4)).asOutput()

        val result = backend.shader(
            Code(
                """
                #version 450
                layout(local_size_x = 1) in;
                layout(std430, binding = 0) buffer Data { int values[]; };
                void main() { values[gl_GlobalInvocationID.x] += 10; }
                """.trimIndent(),
            ),
        ).compile().use { shader -> shader.dispatch(4, buffer).use { it[buffer] } }

        assertThat(result).containsExactly(11, 12, 13, 14)
    }

    @Test
    fun `64 bit storage buffers are processed on the gpu`(backend: VulkanBackend) {
        val doubles = StorageBuffer<DoubleArray>(0).data(doubleArrayOf(0.5, 1.5, -2.25))
        val longs = StorageBuffer<LongArray>(1).data(longArrayOf(1L shl 40, -7L, 3L))

        val result = backend.shader(
            Code(
                """
                #version 450
                #extension GL_ARB_gpu_shader_int64 : enable
                layout(local_size_x = 1) in;
                layout(std430, binding = 0) buffer Doubles { double values[]; } doubles;
                layout(std430, binding = 1) buffer Longs { int64_t values[]; } longs;
                void main() {
                    uint i = gl_GlobalInvocationID.x;
                    doubles.values[i] *= 2.0LF;
                    longs.values[i] += 1l;
                }
                """.trimIndent(),
            ),
        ).compile().use { shader ->
            shader.dispatch(3, doubles.asOutput(), longs.asOutput()).use { it[doubles] to it[longs] }
        }

        assertThat(result.first).containsExactly(1.0, 3.0, -4.5)
        assertThat(result.second).containsExactly((1L shl 40) + 1, -6L, 4L)
    }

    @Test
    fun `uniform buffer object is visible to the shader`(backend: VulkanBackend) {
        val factor = ByteBuffer.allocate(Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .putFloat(3f)
            .array()
        val params = UniformBufferObject(0).data(factor)
        val input = StorageBuffer<FloatArray>(1).data(floatArrayOf(1f, 2f, 3f))
        val output = StorageBuffer<FloatArray>(2).size(3).asOutput()

        val result = backend.shader(
            Code(
                """
                #version 450
                layout(local_size_x = 1) in;
                layout(std140, binding = 0) uniform Params { float factor; };
                layout(std430, binding = 1) readonly buffer In { float values[]; } inBuffer;
                layout(std430, binding = 2) writeonly buffer Out { float values[]; } outBuffer;
                void main() {
                    uint i = gl_GlobalInvocationID.x;
                    outBuffer.values[i] = inBuffer.values[i] * factor;
                }
                """.trimIndent(),
            ),
        ).compile().use { shader -> shader.dispatch(3, params, input, output).use { it[output] } }

        assertThat(result).containsExactly(3f, 6f, 9f)
    }

    @Test
    fun `image is written by the shader and read back`(backend: VulkanBackend) {
        val image = Image2D(0).dimension(4, 3)

        val result = backend.shader(
            Code(
                """
                #version 450
                layout(local_size_x = 1, local_size_y = 1) in;
                layout(rgba8, binding = 0) writeonly uniform image2D img;
                void main() {
                    ivec2 p = ivec2(gl_GlobalInvocationID.xy);
                    imageStore(img, p, vec4(float(p.x) / 3.0, float(p.y) / 2.0, 0.0, 1.0));
                }
                """.trimIndent(),
            ),
        ).compile().use { shader -> shader.dispatch(4, 3, image).use { it[image] } }

        assertThat(result.width).isEqualTo(4)
        assertThat(result.height).isEqualTo(3)
        assertThat(result.data).hasSize(4 * 3 * 4)
        for (y in 0 until 3) {
            for (x in 0 until 4) {
                val offset = (y * 4 + x) * 4
                assertThat(result.data[offset].toInt() and 0xFF).isEqualTo((x / 3f * 255f).roundToInt())
                assertThat(result.data[offset + 1].toInt() and 0xFF).isEqualTo((y / 2f * 255f).roundToInt())
                assertThat(result.data[offset + 2].toInt()).isZero()
                assertThat(result.data[offset + 3].toInt() and 0xFF).isEqualTo(255)
            }
        }
    }

    @Test
    fun `single channel image is written by the shader and read back`(backend: VulkanBackend) {
        val image = Image2D(0).dimension(4, 1).format(Image2D.Format.R8)

        val result = backend.shader(
            Code(
                """
                #version 450
                layout(local_size_x = 1) in;
                layout(r8, binding = 0) writeonly uniform image2D img;
                void main() {
                    ivec2 p = ivec2(gl_GlobalInvocationID.xy);
                    imageStore(img, p, vec4(float(p.x) / 3.0));
                }
                """.trimIndent(),
            ),
        ).compile().use { shader -> shader.dispatch(4, image).use { it[image] } }

        assertThat(result.data.map { it.toInt() and 0xFF }).containsExactly(0, 85, 170, 255)
    }

    @Test
    fun `untouched image is cleared to zero`(backend: VulkanBackend) {
        val image = Image2D(0).dimension(2, 2)

        val result = backend.shader(
            Code(
                """
                #version 450
                layout(local_size_x = 1) in;
                layout(rgba8, binding = 0) writeonly uniform image2D img;
                void main() {}
                """.trimIndent(),
            ),
        ).compile().use { shader -> shader.dispatch(1, image).use { it[image] } }

        assertThat(result.data).containsOnly(0)
    }

    @Test
    fun `atomic counter counts all invocations from its start value`(backend: VulkanBackend) {
        val counter = AtomicCounter(0).start(5)

        val value = backend.shader(
            Code(
                """
                #version 450
                layout(local_size_x = 64) in;
                layout(std430, binding = 0) buffer Counter { uint counter; };
                void main() { atomicAdd(counter, 1u); }
                """.trimIndent(),
            ),
        ).compile().use { shader -> shader.dispatch(4, counter).use { it[counter] } }

        assertThat(value).isEqualTo(5 + 4 * 64)
    }

    @Test
    fun `compiled shader can be dispatched repeatedly`(backend: VulkanBackend) {
        backend.shader(Code(DOUBLE_SHADER)).compile().use { shader ->
            repeat(50) { round ->
                val input = StorageBuffer<FloatArray>(0).data(floatArrayOf(round.toFloat()))
                val output = StorageBuffer<FloatArray>(1).size(1).asOutput()

                val result = shader.dispatch(1, input, output).use { it[output] }

                assertThat(result).containsExactly(round * 2f)
            }
        }
    }

    @Test
    fun `storage buffer state persists across dispatches`(backend: VulkanBackend) {
        val buffer = StorageBuffer<IntArray>(0).data(intArrayOf(0, 10, 20)).asOutput()

        val results = backend.shader(
            Code(
                """
                #version 450
                layout(local_size_x = 1) in;
                layout(std430, binding = 0) buffer Data { int values[]; };
                void main() { values[gl_GlobalInvocationID.x] += 1; }
                """.trimIndent(),
            ),
        ).compile().use { shader ->
            List(3) { _ -> shader.dispatch(3, buffer).use { result -> result[buffer].toList() } }
        }

        assertThat(results).containsExactly(listOf(1, 11, 21), listOf(2, 12, 22), listOf(3, 13, 23))
    }

    @Test
    fun `atomic counter is reset to its start value on every dispatch`(backend: VulkanBackend) {
        val counter = AtomicCounter(0).start(10)

        val values = backend.shader(
            Code(
                """
                #version 450
                layout(local_size_x = 1) in;
                layout(std430, binding = 0) buffer Counter { uint counter; };
                void main() { atomicAdd(counter, 1u); }
                """.trimIndent(),
            ),
        ).compile().use { shader ->
            List(3) { _ -> shader.dispatch(5, counter).use { result -> result[counter] } }
        }

        assertThat(values).containsExactly(15, 15, 15)
    }

    @Test
    fun `result that is never read can be closed`(backend: VulkanBackend) {
        val input = StorageBuffer<FloatArray>(0).data(floatArrayOf(1f))
        val output = StorageBuffer<FloatArray>(1).size(1).asOutput()

        backend.shader(Code(DOUBLE_SHADER)).compile().use { shader ->
            shader.dispatch(1, input, output).close()
        }
    }

    @Test
    fun `data that does not match the shader bindings is rejected`(backend: VulkanBackend) {
        val onlyInput = StorageBuffer<FloatArray>(0).data(floatArrayOf(1f))

        backend.shader(Code(DOUBLE_SHADER)).compile().use { shader ->
            assertThatThrownBy { shader.dispatch(1, onlyInput) }
                .isInstanceOf(KomputeConfigurationException::class.java)
        }
    }

    companion object {
        private val DOUBLE_SHADER = """
            #version 450
            layout(local_size_x = 1) in;
            layout(std430, binding = 0) readonly buffer In { float values[]; } inBuffer;
            layout(std430, binding = 1) writeonly buffer Out { float values[]; } outBuffer;
            void main() {
                uint i = gl_GlobalInvocationID.x;
                outBuffer.values[i] = inBuffer.values[i] * 2.0;
            }
        """.trimIndent()
    }
}
