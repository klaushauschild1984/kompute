package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.Kompute
import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.data.UniformBufferObject
import de.hauschild.kompute.core.shader.ShaderSource.Glsl.Code
import org.junit.jupiter.api.Test

class VulkanBackendCloseTest {
    @Test
    fun `closing the backend releases the resources of results that were never read`() {
        Kompute.vulkan().use { backend ->
            val params = UniformBufferObject(0).data(ByteArray(Float.SIZE_BYTES))
            val image = Image2D(1).dimension(4, 4)
            val output = StorageBuffer<IntArray>(2).size(1).asOutput()

            backend.shader(Code(SHADER)).compile().use { shader ->
                shader.dispatch(1, params, image, output)
            }
        }
    }

    companion object {
        private val SHADER = """
            #version 450
            layout(local_size_x = 1) in;
            layout(std140, binding = 0) uniform Params { float factor; };
            layout(rgba8, binding = 1) writeonly uniform image2D img;
            layout(std430, binding = 2) writeonly buffer Out { int values[]; };
            void main() {
                imageStore(img, ivec2(0, 0), vec4(factor));
                values[0] = 1;
            }
        """.trimIndent()
    }
}
