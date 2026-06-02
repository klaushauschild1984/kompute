package de.hauschild.kompute.opengl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL43

class Program(
    private val shader: Shader,
) : AutoCloseable {
    private var programId: Int = 0

    fun link() {
        programId = GL43.glCreateProgram()
        shader.attach(programId)
        logger.debug { "Linking program" }
        GL43.glLinkProgram(programId)
        if (GL43.glGetProgrami(programId, GL43.GL_LINK_STATUS) == GL11.GL_FALSE) {
            error("Program link error: ${GL43.glGetProgramInfoLog(programId)}")
        }
    }

    fun activate() {
        GL43.glUseProgram(programId)
    }

    override fun close() {
        if (programId == 0) return
        GL43.glDeleteProgram(programId)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
