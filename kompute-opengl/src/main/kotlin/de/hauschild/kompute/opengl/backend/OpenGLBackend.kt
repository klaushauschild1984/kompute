package de.hauschild.kompute.opengl.backend

import de.hauschild.kompute.core.InternalApi
import de.hauschild.kompute.core.backend.AbstractBackend
import de.hauschild.kompute.core.backend.Type
import de.hauschild.kompute.core.exception.requireBackendInitialization
import de.hauschild.kompute.core.shader.CompiledShader
import de.hauschild.kompute.core.shader.ShaderSource
import de.hauschild.kompute.opengl.Limits
import de.hauschild.kompute.opengl.shader.OpenGLCompiledShader
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL31
import org.lwjgl.opengl.GL42
import org.lwjgl.opengl.GL43
import org.lwjgl.system.MemoryUtil

/**
 * OpenGL compute backend implementation using LWJGL.
 *
 * Initializes an offscreen OpenGL 4.3 context via GLFW, compiles and links compute shaders,
 * and manages storage buffer transfer between host and GPU.
 *
 * System property `kompute.backend.egl`: if set, uses EGL instead of WGL for context creation —
 * required on headless systems without a native OpenGL driver (e.g. CI).
 */
class OpenGLBackend : AbstractBackend() {
    private var windowHandle: Long = MemoryUtil.NULL
    private var limits: Limits? = null

    @InternalApi
    override fun type(): Type = Type.OpenGL

    override fun doInitialize() {
        requireBackendInitialization(GLFW.glfwInit()) {
            "Failed to initialize GLFW"
        }
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        val eglActivated = System.getProperty(EGL_PROPERTY) != null
        if(eglActivated){
            logger.debug { "EGL context creation API enabled via $EGL_PROPERTY system property" }
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_EGL_CONTEXT_API)
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API)
        }
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, OPENGL_VERSION_MAJOR)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, OPENGL_VERSION_MINOR)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)

        windowHandle = GLFW.glfwCreateWindow(1, 1, "Kompute", MemoryUtil.NULL, MemoryUtil.NULL)
        requireBackendInitialization(windowHandle != MemoryUtil.NULL) {
            "Failed to create GLFW window"
        }
        GLFW.glfwMakeContextCurrent(windowHandle)
        logger.error { "current GLFW context: ${GLFW.glfwGetCurrentContext()} (expected: $windowHandle)" }
        logger.error { "GLFW error after makeContextCurrent: ${GLFW.glfwGetError(null)}" }
        logger.error { "GL.getFunctionProvider() before create: ${GL.getFunctionProvider()}" }
        if (eglActivated) {
            GL.destroy()
            GL.create { functionName -> GLFW.glfwGetProcAddress(functionName) }
            logger.error { "GL.getFunctionProvider() after create: ${GL.getFunctionProvider()}" }
            logger.error { "glfwGetProcAddress(glGetError): ${GLFW.glfwGetProcAddress("glGetError")}" }
            logger.error { "glfwGetProcAddress(glGetIntegerv): ${GLFW.glfwGetProcAddress("glGetIntegerv")}" }
        }
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
        if (windowHandle == MemoryUtil.NULL) {
            return
        }
        GLFW.glfwDestroyWindow(windowHandle)
        GLFW.glfwTerminate()
    }

    companion object {
        private const val OPENGL_VERSION_MAJOR = 4
        private const val OPENGL_VERSION_MINOR = 3
        private const val EGL_PROPERTY = "kompute.backend.egl"
    }
}
