package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.exception.KomputeConfigurationException

/**
 * Represents an OpenGL buffer that can be read back from the GPU.
 *
 * @param T
 */
internal interface OpenGLReadable<T: Any> {
    /**
     * The source the read data belongs to.
     */
    val source: OutputCapable<T>

    /**
     * Ensures that the GPU has finished writing to the buffer before reading.
     */
    val barrierBit: Int

    /**
     * Reads the buffer contents from the GPU back to host memory.
     *
     * @return the data read from the GPU buffer
     * @throws [KomputeConfigurationException] if the data type is not supported
     */
    fun read(): T
}
