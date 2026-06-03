package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.AbstractBackend
import de.hauschild.kompute.core.ExecutionContext
import de.hauschild.kompute.core.InternalApi
import de.hauschild.kompute.core.ShaderData.StorageBuffer
import de.hauschild.kompute.core.ShaderResult
import de.hauschild.kompute.core.Type
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL43
import org.lwjgl.system.MemoryUtil.NULL

class OpenGLBackend : AbstractBackend() {
    private var window: Long = NULL

    @InternalApi
    override fun type(): Type = Type.OpenGL

    override fun doInitialize() {
        if (!GLFW.glfwInit()) error("Failed to initialize GLFW")
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)

        window = GLFW.glfwCreateWindow(1, 1, "Kompute", NULL, NULL)
        if (window == NULL) error("Failed to create GLFW window")
        GLFW.glfwMakeContextCurrent(window)
        GL.createCapabilities()

        val renderer = GL11.glGetString(GL11.GL_RENDERER)
        val vendor = GL11.glGetString(GL11.GL_VENDOR)
        val openGlVersion = GL11.glGetString(GL11.GL_VERSION)

        logger.info { "OpenGL Backend initialized with renderer: $renderer, vendor: $vendor, version: $openGlVersion" }
    }

    override fun execute(context: ExecutionContext): ShaderResult {
        val results = mutableMapOf<String, FloatArray>()
        OpenGLShader(context.source).use { shader ->
            shader.compile()
            OpenGLProgram(shader).use { program ->
                program.link()

                val storageBuffer = mutableListOf<OpenGLStorageBuffer>()
                context.data.forEach { shaderData ->
                    when (shaderData) {
                        is StorageBuffer -> storageBuffer.add(OpenGLStorageBuffer(shaderData))
                    }
                }

                try {
                    storageBuffer.forEach { buffer -> buffer.bind() }

                    program.activate()

                    logger.debug { "Dispatching computation with (x: ${context.x}, y: ${context.y}, z: ${context.z})" }
                    GL43.glDispatchCompute(context.x, context.y, context.z)
                    GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT)
                    storageBuffer
                        .filter { buffer -> buffer.isOutput() }
                        .forEach { buffer -> results[buffer.outputName()] = buffer.read() }
                } finally {
                    storageBuffer.forEach { it.close() }
                }
            }
        }

        return ShaderResult(results)
    }

    override fun close() {
        logger.debug { "Closing OpenGL Backend" }
        if (window == NULL) return
        GLFW.glfwDestroyWindow(window)
        GLFW.glfwTerminate()
    }
}
