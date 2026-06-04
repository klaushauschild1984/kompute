package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.requireBackendInitialization
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL43

class OpenGLProgram(
    private val shader: OpenGLShader,
) : AutoCloseable {
    private var glHandle: Int = 0

    fun link() {
        glHandle = GL43.glCreateProgram()
        shader.attach(glHandle)
        logger.debug { "Linking program" }
        GL43.glLinkProgram(glHandle)
        requireBackendInitialization(GL43.glGetProgrami(glHandle, GL43.GL_LINK_STATUS) == GL11.GL_TRUE) {
            "Program link error: ${GL43.glGetProgramInfoLog(glHandle)}"
        }
    }

    fun activate() {
        GL43.glUseProgram(glHandle)
    }

    override fun close() {
        if (glHandle == 0) return
        GL43.glDeleteProgram(glHandle)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
