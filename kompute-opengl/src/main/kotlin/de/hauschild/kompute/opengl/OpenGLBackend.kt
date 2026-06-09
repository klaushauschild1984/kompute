package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.backend.AbstractBackend
import de.hauschild.kompute.core.backend.CompiledShader
import de.hauschild.kompute.core.backend.InternalApi
import de.hauschild.kompute.core.backend.Type
import de.hauschild.kompute.core.exception.requireBackendInitialization
import de.hauschild.kompute.core.execution.ShaderSource
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL31
import org.lwjgl.opengl.GL42
import org.lwjgl.opengl.GL43
import org.lwjgl.system.MemoryUtil.NULL

/**
 * OpenGL compute backend implementation using LWJGL.
 *
 * Initializes an offscreen OpenGL 4.3 context via GLFW, compiles and links compute shaders,
 * and manages storage buffer transfer between host and GPU.
 */
class OpenGLBackend : AbstractBackend() {
    private var windowHandle: Long = NULL
    private var limits: Limits? = null

    @InternalApi
    override fun type(): Type = Type.OpenGL

    override fun doInitialize() {
        requireBackendInitialization(GLFW.glfwInit()) {
            "Failed to initialize GLFW"
        }
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, OPENGL_VERSION_MAJOR)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, OPENGL_VERSION_MINOR)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)

        windowHandle = GLFW.glfwCreateWindow(1, 1, "Kompute", NULL, NULL)
        requireBackendInitialization(windowHandle != NULL) {
            "Failed to create GLFW window"
        }
        GLFW.glfwMakeContextCurrent(windowHandle)
        GL.createCapabilities()

        limits = Limits(
            maxShaderStorageBufferBindings = GL11.glGetInteger(GL43.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS),
            maxUniformBufferBindings = GL11.glGetInteger(GL31.GL_MAX_UNIFORM_BUFFER_BINDINGS),
            maxComputeWorkGroupCountX = GL43.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0),
            maxComputeWorkGroupCountY = GL43.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1),
            maxComputeWorkGroupCountZ = GL43.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2),
            maxAtomicCounterBindings = GL11.glGetInteger(GL42.GL_MAX_ATOMIC_COUNTER_BUFFER_BINDINGS),
            maxImageUnits = GL11.glGetInteger(GL42.GL_MAX_IMAGE_UNITS),
            maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE),
        )

        val renderer = GL11.glGetString(GL11.GL_RENDERER)
        val vendor = GL11.glGetString(GL11.GL_VENDOR)
        val openGlVersion = GL11.glGetString(GL11.GL_VERSION)
        logger.info { "OpenGL Backend initialized with renderer: $renderer, vendor: $vendor, version: $openGlVersion" }
    }

    override fun compileSource(source: ShaderSource): CompiledShader {
        requireBackendInitialization(limits != null) { "OpenGL backend is not initialized" }
        val openGLShader = OpenGLShader(source)
        openGLShader.compile()
        val openGLProgram = OpenGLProgram(openGLShader)
        openGLProgram.link()
        return OpenGLCompiledShader(openGLProgram, limits!!)
    }

    override fun close() {
        logger.debug { "Closing OpenGL Backend" }
        if (windowHandle == NULL) {
            return
        }
        GLFW.glfwDestroyWindow(windowHandle)
        GLFW.glfwTerminate()
    }

    companion object {
        private const val OPENGL_VERSION_MAJOR = 4
        private const val OPENGL_VERSION_MINOR = 3
    }
}
