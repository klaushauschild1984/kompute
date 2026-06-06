package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.exception.requireBackendInitialization
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL43

/**
 * Wraps an OpenGL compute program.
 *
 * Links the attached [shader] into an executable compute program and activates it for dispatch.
 * Must be used within a `use` block — releases the OpenGL program handle on close.
 *
 * @param shader the compiled shader to link into this program
 */
class OpenGLProgram(
    private val shader: OpenGLShader,
) : AutoCloseable {
    private var glHandle: Int = 0

    /**
     * Links the attached shader into an executable OpenGL compute program.
     *
     * @throws [KomputeBackendInitializationException] if linking fails
     */
    fun link() {
        glHandle = GL43.glCreateProgram()
        shader.attach(glHandle)
        logger.debug { "Linking program" }
        GL43.glLinkProgram(glHandle)
        requireBackendInitialization(GL43.glGetProgrami(glHandle, GL43.GL_LINK_STATUS) == GL11.GL_TRUE) {
            "Program link error: ${GL43.glGetProgramInfoLog(glHandle)}"
        }
    }

    /**
     * Activates this program for subsequent compute dispatch.
     */
    fun activate() {
        GL43.glUseProgram(glHandle)
    }

    override fun close() {
        if (glHandle == 0) {
            return
        }
        GL43.glDeleteProgram(glHandle)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
