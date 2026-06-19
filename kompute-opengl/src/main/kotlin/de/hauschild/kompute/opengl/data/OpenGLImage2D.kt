package de.hauschild.kompute.opengl.data

import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.data.Image2D.Format
import de.hauschild.kompute.core.data.Image2D.Image2DResult
import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.exception.requireConfiguration
import de.hauschild.kompute.opengl.Buffer
import de.hauschild.kompute.opengl.Readable
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL42
import org.lwjgl.opengl.GL43
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * Wraps an OpenGL image2D for a [Image2D].
 *
 * @param source the [Image2D] configuration this image is based on
 */
class OpenGLImage2D(
    source: Image2D
) : Buffer<Image2D>(source),
OutputCapable<Image2DResult> by source,
Readable<Image2DResult> {
    override val barrierBit: Int = GL43.GL_TEXTURE_UPDATE_BARRIER_BIT
    private val openGLFormat = OpenGLFormat(source.format)

    /**
     * Validates the image size against the maximum texture size.
     *
     * @param maxTextureSize maximum texture size
     */
    fun validateTextureSize(maxTextureSize: Int) {
        requireConfiguration(source.width!! <= maxTextureSize&&
                source.height!! <= maxTextureSize){
            "Image2D size (${source.width}x${source.height}) exceeds maximum texture size ($maxTextureSize)"
        }
    }

    override fun bind() {
        glHandle = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glHandle)

        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D, 0,
            openGLFormat.internalFormat,
            source.width!!,
            source.height!!,
            0,
            openGLFormat.pixelFormat,
            GL11.GL_UNSIGNED_BYTE,
            null as ByteBuffer?
        )
        val zeros = MemoryUtil.memCalloc(source.width!! * source.height!! * source.format.bytesPerPixel)
        try {
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0,
                source.width!!, source.height!!,
                openGLFormat.pixelFormat, GL11.GL_UNSIGNED_BYTE, zeros)
        } finally {
            MemoryUtil.memFree(zeros)
        }

        GL42.glBindImageTexture(
            source.index,
            glHandle,
            0,
            false,
            0,
            GL42.GL_WRITE_ONLY,
            openGLFormat.internalFormat
        )
    }

    override fun read(): Image2DResult {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glHandle)
        val bytes = ByteArray(source.width!! * source.height!! * source.format.bytesPerPixel)
        val buffer = MemoryUtil.memAlloc(bytes.size)
        try {
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D,
                0,
                openGLFormat.pixelFormat,
                GL11.GL_UNSIGNED_BYTE,
                buffer)
            buffer.rewind()
            buffer.get(bytes)
        } finally {
            MemoryUtil.memFree(buffer)
        }
        return Image2DResult(bytes, source.width!!, source.height!!, source.format)
    }

    override fun close() {
        if (glHandle == 0) {
            return
        }
        GL11.glDeleteTextures(glHandle)
    }

    /**
     * OpenGL format mapping.
     *
     * @property internalFormat OpenGL internal format
     * @property pixelFormat OpenGL pixel format
     */
    class OpenGLFormat(
        val internalFormat: Int,
        val pixelFormat: Int,
    ){
        companion object {
            /**
             * Creates an [OpenGLFormat] from a [Format].
             *
             * @param format the [Format] to convert
             */
            operator fun invoke(format: Format) = when (format) {
                Format.RGBA8 -> OpenGLFormat(GL11.GL_RGBA8, GL11.GL_RGBA)
                Format.R8 -> OpenGLFormat(GL30.GL_R8, GL11.GL_RED)
            }
        }
    }
}
