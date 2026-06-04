package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.ShaderSource
import de.hauschild.kompute.core.requireBackendInitialization
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL43

class OpenGLShader(
    private val source: ShaderSource,
) : AutoCloseable {
    private var glHandle: Int = 0

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
        glHandle = GL43.glCreateShader(GL43.GL_COMPUTE_SHADER)
        GL43.glShaderSource(glHandle, glsl)
        logger.debug { "Compiling shader" }
        GL43.glCompileShader(glHandle)
        requireBackendInitialization(GL43.glGetShaderi(glHandle, GL43.GL_COMPILE_STATUS) == GL11.GL_TRUE) {
            "Shader compile error: ${GL43.glGetShaderInfoLog(glHandle)}"
        }
    }

    fun attach(programId: Int) {
        GL43.glAttachShader(programId, glHandle)
    }

    override fun close() {
        if (glHandle == 0) return
        GL43.glDeleteShader(glHandle)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
