package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.backend.AbstractCompiledShader
import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.data.NamedUniform
import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.data.UniformBufferObject
import de.hauschild.kompute.core.exception.requireConfiguration
import de.hauschild.kompute.core.execution.ShaderResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL43

/**
 * OpenGL implementation of a compiled compute shader.
 *
 * Holds a linked [OpenGLProgram] and the GPU [Limits] for runtime validation.
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
     * Validates, activates the shader program, binds all data, runs the compute
     * dispatch, and returns the GPU outputs.
     *
     * @param x number of work groups in the X dimension — must be ≥ 1 and ≤ [Limits.maxComputeWorkGroupCountX]
     * @param y number of work groups in the Y dimension — must be ≥ 1 and ≤ [Limits.maxComputeWorkGroupCountY]
     * @param z number of work groups in the Z dimension — must be ≥ 1 and ≤ [Limits.maxComputeWorkGroupCountZ]
     * @param data shader inputs and outputs — at least one output required
     * @return the results of all output buffers after the dispatch completes
     */
    override fun dispatch(
        x: Int,
        y: Int,
        z: Int,
        vararg data: ShaderData
    ): ShaderResult {
        validateDispatch(x, y, z, *data)
        program.activate()
        val buffers = bindBuffers(data.toList())
        return ShaderResult(readBack(buffers, x,y,z))
    }

    private fun bindBuffers(data: List<ShaderData>): List<Bindable> {
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
        return storageBuffer + uniformBufferObjects + namedUniforms + atomicCounters + image2Ds
    }

    private fun readBack(
        buffers: List<Bindable>,
        x:Int,
        y:Int,
        z:Int,
    ): MutableMap<OutputCapable<*>, Any> {
        val results = mutableMapOf<OutputCapable<*>, Any>()
        try {
            buffers.forEach { buffer -> buffer.bind() }

            logger.debug { "Dispatching computation with (x: $x, y: $y, z: $z)" }
            GL43.glDispatchCompute(x, y, z)
            GL43.glMemoryBarrier(
                buffers.filterIsInstance<Readable<*>>()
                    .fold(0) { acc, readable -> acc or readable.barrierBit }
            )

            logger.debug { "Read back results from GPU" }
            buffers.filterIsInstance<Readable<*>>()
                .filter { buffer -> buffer.source.isOutput }
                .forEach { buffer -> results[buffer.source] = buffer.read() }

            return results
        } finally {
            buffers.forEach { it.close() }
        }
    }

    override fun close() {
        program.close()
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
