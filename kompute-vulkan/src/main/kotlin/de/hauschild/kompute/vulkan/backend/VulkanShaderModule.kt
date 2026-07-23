package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.KomputeShaderSourceException
import de.hauschild.kompute.core.shader.ShaderSource
import org.lwjgl.util.shaderc.Shaderc.shaderc_compilation_status_success
import org.lwjgl.util.shaderc.Shaderc.shaderc_compile_into_spv
import org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_initialize
import org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_release
import org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_compute_shader
import org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_bytes
import org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_compilation_status
import org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_error_message
import org.lwjgl.util.shaderc.Shaderc.shaderc_result_release

/**
 * A Vulkan shader module handling compilation and pipeline setup.
 *
 * @param source the [ShaderSource]
 */
class VulkanShaderModule(source: ShaderSource) : AutoCloseable{
    private val compiler = shaderc_compiler_initialize()
    private var result: Long = -1

    init {
        val shaderBytecode: ByteArray = when (source) {
            is ShaderSource.Glsl -> compile(source.resolve())
            is ShaderSource.Spirv -> source.resolve()
        }
        TODO()
    }

    private fun compile(glsl: String): ByteArray {
        result = shaderc_compile_into_spv(
            compiler, glsl, shaderc_glsl_compute_shader, "shader", "main", 0
        )
        if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
            val errorMessage = shaderc_result_get_error_message(result)
            throw KomputeShaderSourceException("Failed to compile shader: $errorMessage")
        }
        val byteBuffer = shaderc_result_get_bytes(result)!!
        return ByteArray(byteBuffer.remaining()).also { byteBuffer.get(it)}
    }

    override fun close() {
        if (result != -1L) {
            shaderc_result_release(result)
        }
        shaderc_compiler_release(compiler)
    }
}
