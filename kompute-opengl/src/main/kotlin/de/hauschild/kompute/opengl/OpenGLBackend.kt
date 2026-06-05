package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.AbstractBackend
import de.hauschild.kompute.core.ExecutionContext
import de.hauschild.kompute.core.InternalApi
import de.hauschild.kompute.core.ShaderData.OutputCapable
import de.hauschild.kompute.core.ShaderData.StorageBuffer
import de.hauschild.kompute.core.ShaderData.UniformBuffer
import de.hauschild.kompute.core.ShaderResult
import de.hauschild.kompute.core.Type
import de.hauschild.kompute.core.requireBackendInitialization
import de.hauschild.kompute.core.requireConfiguration
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
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
    private var maxShaderStorageBufferBindings: Int = 0
    private var maxUniformBufferBindings: Int = 0
    private var maxComputeWorkGroupCountX: Int = 0
    private var maxComputeWorkGroupCountY: Int = 0
    private var maxComputeWorkGroupCountZ: Int = 0

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

        maxShaderStorageBufferBindings = GL11.glGetInteger(GL43.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS)
        maxUniformBufferBindings = GL11.glGetInteger(GL43.GL_MAX_UNIFORM_BUFFER_BINDINGS)
        maxComputeWorkGroupCountX = GL43.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0)
        maxComputeWorkGroupCountY = GL43.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1)
        maxComputeWorkGroupCountZ = GL43.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2)

        val renderer = GL11.glGetString(GL11.GL_RENDERER)
        val vendor = GL11.glGetString(GL11.GL_VENDOR)
        val openGlVersion = GL11.glGetString(GL11.GL_VERSION)
        logger.info { "OpenGL Backend initialized with renderer: $renderer, vendor: $vendor, version: $openGlVersion" }
    }

    override fun dispatch(context: ExecutionContext): ShaderResult {
        // TODO: support multi-dispatch — reuse the linked program across multiple glDispatchCompute calls
        OpenGLShader(context.source).use { shader ->
            shader.compile()
            OpenGLProgram(shader).use { program ->
                program.link()
                program.activate()

                return ShaderResult(dispatchBuffers(context))
            }
        }
    }

    private fun dispatchBuffers(context: ExecutionContext): Map<OutputCapable<*>, Any> {
        requireConfiguration(context.x <= maxComputeWorkGroupCountX) {
            "Work group count x must not exceed physical limit $maxComputeWorkGroupCountX"
        }
        requireConfiguration(context.y <= maxComputeWorkGroupCountY) {
            "Work group count y must not exceed physical limit $maxComputeWorkGroupCountY"
        }
        requireConfiguration(context.z <= maxComputeWorkGroupCountZ) {
            "Work group count z must not exceed physical limit $maxComputeWorkGroupCountZ"
        }

        val results = mutableMapOf<OutputCapable<*>, Any>()

        val storageBuffer = mutableListOf<OpenGLStorageBuffer<*>>()
        val uniformBuffers = mutableListOf<OpenGLUniformBuffer>()
        context.data.forEach { shaderData ->
            when (shaderData) {
                is StorageBuffer<*> -> {
                    val openGLStorageBuffer = OpenGLStorageBuffer(shaderData)
                    openGLStorageBuffer.validate(maxShaderStorageBufferBindings)
                    storageBuffer.add(openGLStorageBuffer)
                }
                is UniformBuffer -> {
                    val openGLUniformBuffer = OpenGLUniformBuffer(shaderData)
                    openGLUniformBuffer.validate(maxUniformBufferBindings)
                    uniformBuffers.add(openGLUniformBuffer)
                }
            }
        }
        val buffers = storageBuffer + uniformBuffers

        try {
            buffers.forEach { buffer -> buffer.bind() }

            logger.debug { "Dispatching computation with (x: ${context.x}, y: ${context.y}, z: ${context.z})" }
            GL43.glDispatchCompute(context.x, context.y, context.z)
            GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT)
            storageBuffer
                .filter { buffer -> buffer.isOutput }
                .forEach { buffer -> results[buffer.source] = buffer.read() }

            return results
        } finally {
            buffers.forEach { it.close() }
        }
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
