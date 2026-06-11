package de.hauschild.kompute.opengl.backend

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import java.lang.AutoCloseable

/**
 * Represents the available context creation strategies.
 *
 * System property `kompute.backend.egl`: if set, uses EGL instead of WGL for context creation —
 * required on headless systems without a native OpenGL driver (e.g. CI).
 */
sealed interface ContextCreationStrategy: AutoCloseable {
    /**
     * Default [ContextCreationStrategy] where nothing additional is done.
     */
    object Wgl : ContextCreationStrategy {
        override fun windowHints() {
            // nothing to do
        }

        override fun contextCreation() {
            // nothing to do
        }
        override fun close() {
            // nothing to do
        }
    }

    /**
     * [ContextCreationStrategy] that uses EGL for context creation.
     */
    object Egl : ContextCreationStrategy {
        override fun windowHints() {
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_EGL_CONTEXT_API)
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API)
        }

        override fun contextCreation() {
            GL.destroy()
            GL.create { functionName -> GLFW.glfwGetProcAddress(functionName) }
        }
        override fun close() {
            GL.destroy()
        }
    }

    /**
     * Applies strategy-specific window hints.
     */
    fun windowHints()

    /**
     * Applies strategy-specific context creation.
     */
    fun contextCreation()

    companion object {
        private const val EGL_PROPERTY = "kompute.backend.egl"
        private val logger = KotlinLogging.logger {}

        /**
         * Get the context creation strategy to use. Defaults to WGL.
         *
         * @return [Wgl] by default and [Egl] if the system property `kompute.backend.egl` is set
         */
        fun get(): ContextCreationStrategy {
            val eglActivated = System.getProperty(EGL_PROPERTY) != null
            if(eglActivated){
                logger.debug { "EGL context creation API enabled via $EGL_PROPERTY system property" }
                return Egl
            }
            return Wgl
        }
    }
}
