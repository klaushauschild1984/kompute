package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.AbstractBackend
import de.hauschild.kompute.core.ExecutionContext
import de.hauschild.kompute.core.InternalApi
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

        logger.debug { "OpenGL Backend initialized with renderer: $renderer, vendor: $vendor, version: $openGlVersion" }
    }

    override fun execute(context: ExecutionContext): ShaderResult {
        val results = mutableMapOf<String, FloatArray>()
        Shader(context.source).use { shader ->
            shader.compile()
            Program(shader).use { program ->
                program.link()

                val inputs =
                    context.inputs.map { (index, data) ->
                        val buffer = Buffer(index, data)
                        buffer.bindAsInput()
                        buffer
                    }
                val outputs =
                    context.outputs.mapValues { (binding, data) ->
                        val (index, name) = binding
                        val buffer = Buffer(index, data, name)
                        buffer.bindAsOutput()
                        buffer
                    }

                program.activate()

                logger.debug { "Dispatching computation with (x: ${context.x}, y: ${context.y}, z: ${context.z})" }
                GL43.glDispatchCompute(context.x, context.y, context.z)
                GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT)

                inputs.forEach { it.close() }
                outputs.forEach { (binding, buffer) ->
                    val (_, name) = binding
                    results[name] = buffer.read()
                    buffer.close()
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
