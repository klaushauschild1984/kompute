package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.ShaderSource
import de.hauschild.kompute.core.requireBackendInitialization
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL43

/**
 * Wraps an OpenGL compute shader object.
 *
 * Compiles the shader from a [ShaderSource] and attaches it to an OpenGL program.
 * Must be used within a `use` block — releases the OpenGL shader handle on close.
 *
 * @param source the shader source to compile
 */
class OpenGLShader(
    private val source: ShaderSource,
) : AutoCloseable {
    private var glHandle: Int = 0

    /**
     * Compiles the shader source into an OpenGL compute shader.
     *
     * @throws de.hauschild.kompute.core.KomputeBackendInitializationException if compilation fails
     */
    fun compile() {
        val glsl =
            when (source) {
                is ShaderSource.Code -> {
                    logger.debug { "Reading shader from code" }
                    logger.trace {
                        """
                        ${source.glsl}
                        """.trimIndent()
                    }
                    source.glsl
                }
                is ShaderSource.File -> {
                    logger.debug { "Reading shader from file ${source.path}" }
                    source.path.toFile().readText()
                }
                is ShaderSource.Stream -> {
                    logger.debug { "Reading shader from stream" }
                    source.inputStream.use { it.reader().readText() }
                }
            }
        // TODO: cache compiled shader handles by source hash to avoid redundant recompilation
        // TODO: support pre-compilation (glProgramBinary / GL_ARB_get_program_binary) for faster startup
        glHandle = GL43.glCreateShader(GL43.GL_COMPUTE_SHADER)
        GL43.glShaderSource(glHandle, glsl)
        logger.debug { "Compiling shader" }
        GL43.glCompileShader(glHandle)
        requireBackendInitialization(GL43.glGetShaderi(glHandle, GL43.GL_COMPILE_STATUS) == GL11.GL_TRUE) {
            "Shader compile error: ${GL43.glGetShaderInfoLog(glHandle)}"
        }
    }

    /**
     * Attaches this shader to the given OpenGL program.
     *
     * @param programId the OpenGL handle of the program to attach to
     */
    fun attach(programId: Int) {
        GL43.glAttachShader(programId, glHandle)
    }

    override fun close() {
        if (glHandle == 0) {
            return
        }
        GL43.glDeleteShader(glHandle)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
