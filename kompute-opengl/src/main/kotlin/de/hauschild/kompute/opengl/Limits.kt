package de.hauschild.kompute.opengl

/**
 * OpenGL GPU limits queried once during backend initialization.
 *
 * Used by [de.hauschild.kompute.opengl.shader.OpenGLCompiledShader] and the OpenGL buffer wrappers to validate
 * dispatch parameters and binding indices against the physical hardware limits
 * before submitting work to the GPU.
 *
 * @property maxShaderStorageBufferBindings Maximum number of shader storage buffer binding points
 * (`GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS`).
 * @property maxUniformBufferBindings Maximum number of uniform buffer binding points
 * (`GL_MAX_UNIFORM_BUFFER_BINDINGS`).
 * @property maxComputeWorkGroupCountX Maximum number of work groups dispatchable in the X dimension
 * (`GL_MAX_COMPUTE_WORK_GROUP_COUNT`).
 * @property maxComputeWorkGroupCountY Maximum number of work groups dispatchable in the Y dimension
 * (`GL_MAX_COMPUTE_WORK_GROUP_COUNT`).
 * @property maxComputeWorkGroupCountZ Maximum number of work groups dispatchable in the Z dimension
 * (`GL_MAX_COMPUTE_WORK_GROUP_COUNT`).
 * @property maxAtomicCounterBindings Maximum number of atomic counter buffer binding points
 * (`GL_MAX_ATOMIC_COUNTER_BUFFER_BINDINGS`).
 * @property maxImageUnits Maximum number of image units available to the shader (`GL_MAX_IMAGE_UNITS`).
 * @property maxTextureSize Maximum width and height of a 2D texture (`GL_MAX_TEXTURE_SIZE`).
 */
data class Limits(
    val maxShaderStorageBufferBindings: Int,

    val maxUniformBufferBindings: Int,

    val maxComputeWorkGroupCountX: Int,

    val maxComputeWorkGroupCountY: Int,

    val maxComputeWorkGroupCountZ: Int,

    val maxAtomicCounterBindings: Int,

    val maxImageUnits: Int,

    val maxTextureSize: Int,
)
