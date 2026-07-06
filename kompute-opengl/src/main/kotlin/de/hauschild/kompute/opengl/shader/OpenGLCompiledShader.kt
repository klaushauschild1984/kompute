package de.hauschild.kompute.opengl.shader

import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.data.NamedUniform
import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.data.UniformBufferObject
import de.hauschild.kompute.core.exception.requireConfiguration
import de.hauschild.kompute.core.result.ShaderResult
import de.hauschild.kompute.core.shader.AbstractCompiledShader
import de.hauschild.kompute.opengl.Bindable
import de.hauschild.kompute.opengl.Limits
import de.hauschild.kompute.opengl.OpenGLResultReader
import de.hauschild.kompute.opengl.Readable
import de.hauschild.kompute.opengl.backend.OpenGLProgram
import de.hauschild.kompute.opengl.data.OpenGLAtomicCounter
import de.hauschild.kompute.opengl.data.OpenGLImage2D
import de.hauschild.kompute.opengl.data.OpenGLNamedUniform
import de.hauschild.kompute.opengl.data.OpenGLStorageBuffer
import de.hauschild.kompute.opengl.data.OpenGLUniformBufferObject
import org.lwjgl.opengl.GL43

/**
 * OpenGL implementation of [de.hauschild.kompute.core.shader.CompiledShader].
 *
 * Holds a linked [OpenGLProgram] and the GPU [de.hauschild.kompute.opengl.Limits] for runtime validation.
 * On [dispatch], the program is activated, all [de.hauschild.kompute.core.data.ShaderData]
 * is wrapped in OpenGL buffer objects, the compute shader is submitted, a memory
 * barrier synchronizes the GPU, and all output buffers are read back to the CPU.
 *
 * Must be closed after use to release the underlying GPU program.
 *
 * @param program the linked OpenGL program to activate and dispatch
 * @param limits the GPU hardware limits used for dispatch validation
 */
class OpenGLCompiledShader(
    private val program: OpenGLProgram,
    private val limits: Limits,
) : AbstractCompiledShader() {
    /**
     * Extends base validation with OpenGL hardware limits.
     *
     * Calls [AbstractCompiledShader.validateDispatch] first, then checks that
     * the workgroup counts do not exceed [Limits.maxComputeWorkGroupCountX],
     * [Limits.maxComputeWorkGroupCountY], and [Limits.maxComputeWorkGroupCountZ].
     */
    override fun validateDispatch(
        x: Int,
        y: Int,
        z: Int,
        vararg data: ShaderData
    ) {
        super.validateDispatch(x,y, z,*data)
        requireConfiguration(x <= limits.maxComputeWorkGroupCountX) {
            "Work group count x must not exceed physical limit ${limits.maxComputeWorkGroupCountX}"
        }
        requireConfiguration(y <= limits.maxComputeWorkGroupCountY) {
            "Work group count y must not exceed physical limit ${limits.maxComputeWorkGroupCountY}"
        }
        requireConfiguration(z <= limits.maxComputeWorkGroupCountZ) {
            "Work group count z must not exceed physical limit ${limits.maxComputeWorkGroupCountZ}"
        }
    }

    /**
     * Activates the shader program, binds all data, runs the compute dispatch, and returns
     * the GPU outputs. Called by [AbstractCompiledShader.dispatch] after validation.
     *
     * @param x number of work groups in the X dimension — must be ≥ 1 and ≤ [Limits.maxComputeWorkGroupCountX]
     * @param y number of work groups in the Y dimension — must be ≥ 1 and ≤ [Limits.maxComputeWorkGroupCountY]
     * @param z number of work groups in the Z dimension — must be ≥ 1 and ≤ [Limits.maxComputeWorkGroupCountZ]
     * @param data shader inputs and outputs, already validated
     * @return the results of all output buffers after the dispatch completes
     */
    override fun doDispatch(
        x: Int,
        y: Int,
        z: Int,
        data: List<ShaderData>
    ): ShaderResult {
        program.activate()
        val bindables = bind(data)
        dispatch(x, y, z, bindables)
        return ShaderResult(OpenGLResultReader(bindables))
    }

    private fun bind(data: List<ShaderData>): List<Bindable> {
        val storageBuffer = mutableListOf<OpenGLStorageBuffer<*>>()
        val uniformBufferObjects = mutableListOf<OpenGLUniformBufferObject>()
        val namedUniforms = mutableListOf<OpenGLNamedUniform<*>>()
        val atomicCounters = mutableListOf<OpenGLAtomicCounter>()
        val image2Ds = mutableListOf<OpenGLImage2D>()
        data.forEach { shaderData ->
            when (shaderData) {
                is StorageBuffer<*> -> {
                    val openGLStorageBuffer = OpenGLStorageBuffer(shaderData)
                    openGLStorageBuffer.validate(limits.maxShaderStorageBufferBindings)
                    storageBuffer.add(openGLStorageBuffer)
                }

                is UniformBufferObject -> {
                    val openGLUniformBufferObject = OpenGLUniformBufferObject(shaderData)
                    openGLUniformBufferObject.validate(limits.maxUniformBufferBindings)
                    uniformBufferObjects.add(openGLUniformBufferObject)
                }

                is NamedUniform<*> -> {
                    val openGLNamedUniform = OpenGLNamedUniform(program, shaderData)
                    namedUniforms.add(openGLNamedUniform)
                }

                is AtomicCounter -> {
                    val openGLAtomicCounter = OpenGLAtomicCounter(shaderData)
                    openGLAtomicCounter.validate(limits.maxAtomicCounterBindings)
                    atomicCounters.add(openGLAtomicCounter)
                }

                is Image2D -> {
                    val openGLImage2D = OpenGLImage2D(shaderData)
                    openGLImage2D.validate(limits.maxImageUnits)
                    openGLImage2D.validateTextureSize(limits.maxTextureSize)
                    image2Ds.add(openGLImage2D)
                }
            }
        }
        val bindable = storageBuffer + uniformBufferObjects + namedUniforms + atomicCounters + image2Ds
        bindable.forEach { bindable -> bindable.bind() }
        return bindable
    }

    private fun dispatch(
        x:Int,
        y:Int,
        z:Int,
        bindables: List<Bindable>
    ) {
        GL43.glDispatchCompute(x, y, z)
        GL43.glMemoryBarrier(
            bindables.filterIsInstance<Readable<*>>()
                .fold(0) { acc, readable -> acc or readable.barrierBit }
        )
    }

    override fun close() {
        program.close()
    }
}
