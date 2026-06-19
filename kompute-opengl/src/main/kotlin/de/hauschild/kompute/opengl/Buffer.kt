package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.IndexedBinding
import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.exception.requireConfiguration
import org.lwjgl.opengl.GL43

/**
 * Wraps an OpenGL buffer object.
 *
 * @param T the [ShaderData] + [de.hauschild.kompute.core.data.IndexedBinding] type this buffer wraps
 * @property source the shader data configuration this buffer is based on
 */
abstract class Buffer<T>(
    val source: T
): Bindable
where T : ShaderData, T : IndexedBinding{
    /**
     * The OpenGL buffer object handle.
     */
    internal var glHandle: Int = 0

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

    /**
     * Deletes the underlying GL buffer object.
     *
     * Subclasses that manage deletion directly (e.g. transient buffers) should call this from [close].
     * Subclasses using [de.hauschild.kompute.opengl.data.GlBufferCache] must override [close] with a no-op instead.
     */
    protected fun deleteGlBuffer() {
        if (glHandle == 0) {
            return
        }
        GL43.glDeleteBuffers(glHandle)
    }

    abstract override fun close()
}
