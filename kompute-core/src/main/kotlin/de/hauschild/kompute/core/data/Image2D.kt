package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.data.Image2D.Image2DResult
import de.hauschild.kompute.core.exception.requireConfiguration
import java.awt.image.BufferedImage

/**
 * A 2D image that a compute shader writes to via `imageStore`.
 *
 * Image2D is always output-only — the shader writes pixel data which is read back after dispatch
 * as an [Image2DResult]. The result always contains the raw pixel bytes and, for formats that
 * support it, can be converted to a [BufferedImage].
 *
 * Declared in GLSL with `layout(binding = N, <format>) uniform writeonly image2D`:
 * ```glsl
 * layout(binding = 0, rgba8) uniform writeonly image2D outputImage;
 *
 * void main() {
 *     imageStore(outputImage, ivec2(gl_GlobalInvocationID.xy), vec4(r, g, b, 1.0));
 * }
 * ```
 *
 * Kotlin:
 * ```kotlin
 * val image = Image2D(0).dimension(1024, 768).format(Image2D.Format.RGBA8)
 * val result = backend.shader(...).compile().use { it.dispatch(x, y, image) }
 * val bufferedImage = result[image].toBufferedImage()
 * ```
 *
 * @property index the binding index in the shader — must be non-negative
 */
class Image2D(
    override val index: Int,
): ShaderData,
IndexedBinding,
OutputCapable<Image2DResult>{
    override val isOutput: Boolean = true

    /**
     * The width of the image.
     */
    var width: Int? = null
        private set

    /**
     * The height of the image.
     */
    var height: Int? = null
        private set

    /**
     * The pixel format of the image.
     */
    var format: Format = Format.RGBA8
        private set

    /**
     * Sets the dimension of the image.
     *
     * @param width 2D image width
     * @param height 2D image height
     * @return this [Image2D] for chaining
     */
    fun dimension(width: Int, height: Int): Image2D {
        this.width = width
        this.height = height
        return this
    }

    /**
     * Sets the pixel format of the image. Defaults to [Format.RGBA8].
     *
     * @param format the pixel format to use for this image
     * @return this [Image2D] for chaining
     */
    fun format(format: Format): Image2D {
        this.format = format
        return this
    }

    override fun validate() {
        super.validate()
        requireConfiguration(width != null && height != null) {
            "Image dimensions must be set"
        }
        requireConfiguration(width!! > 0 && height!! > 0) {
            "Image dimensions must be positive"
        }
    }

    override fun toString(): String = buildString {
        append("Image2D($index)")
        if (width != null && height != null) {
            append(".dimension($width, $height)")
        }
        if (format !is Format.RGBA8) {
            append(".format(${format::class.simpleName})")
        }
    }

    /**
     * Holds the pixel data read back from a [Image2D] after compute shader execution.
     *
     * Always provides access to the raw [data] bytes. For formats that map to a standard
     * Java image type, [toBufferedImage] converts the raw bytes to a [BufferedImage].
     *
     * @property data the raw pixel bytes as returned by the GPU
     * @property width the image width in pixels
     * @property height the image height in pixels
     * @property format the format the image was rendered with
     */
    class Image2DResult(
        val data: ByteArray,
        val width: Int,
        val height: Int,
        val format: Format,
    ) {
        /**
         * Converts the raw image data into a [BufferedImage], if possible (see [Format.bufferedImageType])
         *
         * @return the [BufferedImage] or null if the format is not supported
         */
        fun toBufferedImage(): BufferedImage?{
            format.bufferedImageType ?: return null
            val image = BufferedImage(width, height, format.bufferedImageType)
            when (format) {
                is Format.RGBA8 -> {
                    val pixels = IntArray(width * height) { i ->
                        val r = data[i * 4 + 0].toInt() and 0xFF
                        val g = data[i * 4 + 1].toInt() and 0xFF
                        val b = data[i * 4 + 2].toInt() and 0xFF
                        val a = data[i * 4 + 3].toInt() and 0xFF
                        (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                    image.raster.setDataElements(0, 0, width, height, pixels)
                }
                is Format.R8 -> image.raster.setDataElements(0, 0, width, height, data)
            }
            return image
        }
    }

    /**
     * The pixel format of an [Image2D].
     *
     * The format determines the GLSL image format qualifier used in the shader declaration,
     * the OpenGL internal texture format, and — if applicable — the corresponding
     * [BufferedImage] type for result conversion.
     *
     * The GLSL format qualifier must match exactly:
     * ```glsl
     * layout(binding = 0, rgba8) uniform writeonly image2D img;  // Format.RGBA8
     * layout(binding = 0, r8)    uniform writeonly image2D img;  // Format.R8
     * ```
     *
     * @property bytesPerPixel the number of bytes per pixel in the image data
     * @property bufferedImageType the [BufferedImage] `TYPE_*` constant for this format,
     * or `null` if conversion to [BufferedImage] is not supported
     */
    sealed class Format(val bytesPerPixel: Int, val bufferedImageType: Int?) {
        /**
         *  4-channel RGBA, 8 bits per channel. GLSL qualifier: `rgba8`.
         *  Converts to [java.awt.image.BufferedImage.TYPE_INT_ARGB].
         */
        object RGBA8 : Format(4,BufferedImage.TYPE_INT_ARGB)

        /**
         *  Single-channel grayscale, 8 bits per pixel. GLSL qualifier: `r8`.
         *  Converts to [java.awt.image.BufferedImage.TYPE_BYTE_GRAY].
         */
        object R8 : Format(1,BufferedImage.TYPE_BYTE_GRAY)
    }
}
