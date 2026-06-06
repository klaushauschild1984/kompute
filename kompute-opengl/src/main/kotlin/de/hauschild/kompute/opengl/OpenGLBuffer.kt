package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.IndexedBinding
import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.exception.requireConfiguration
import org.lwjgl.opengl.GL43

/**
 * Wraps an OpenGL buffer object.
 *
 * @param T
 * @property source
 */
abstract class OpenGLBuffer<T>(
    val source: T
): Bindable
where T : ShaderData, T : IndexedBinding{
    /**
     * The OpenGL buffer object handle.
     */
    protected var glHandle: Int = 0

    /**
     * Validates that the buffer's binding index is within the GPU's supported range.
     *
     * @param maxBindings the maximum number of buffer bindings supported by the GPU
     * @throws [KomputeConfigurationException] if the index exceeds the limit
     */
    fun validate(maxBindings: Int) {
        requireConfiguration(source.index < maxBindings) {
            "Buffer index ${source.index} exceeds maximum binding index ${maxBindings - 1}"
        }
    }

    override fun close() {
        if (glHandle == 0) {
            return
        }
        GL43.glDeleteBuffers(glHandle)
    }
}
