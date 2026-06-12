package de.hauschild.kompute.showcase

import de.hauschild.kompute.core.Kompute
import de.hauschild.kompute.core.backend.Backend
import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.data.Image2D.Format
import de.hauschild.kompute.core.data.NamedUniform
import de.hauschild.kompute.core.shader.ShaderSource.Code
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.image.BufferedImage
import kotlin.time.TimeSource

/**
 * A Mandelbrot set renderer that uses a GPU shader to render the set.
 *
 * @param backend the [Backend] to be used for rendering - defaults to [Kompute.openGL]
 * @param closeBackend whether to close the backend after use - defaults to true
 */
class MandelbrotRenderer(
    backend: Backend = Kompute.openGL(),
    closeBackend: Boolean = true,
) : BackendUser(backend, closeBackend) {
    private val shaderCode = checkNotNull(
        MandelbrotRenderer::class.java.getResourceAsStream(SHADER_FILE)
    ) {
        "Shader $SHADER_FILE not found"
    }.use { it.reader().readText().replace("\$LOCAL_SIZE", LOCAL_SIZE.toString()) }
    private val compiledShader = backend.shader(Code(shaderCode)).compile()

    /**
     * Renders the Mandelbrot set using the provided configuration to a [BufferedImage].
     *
     * @param config the configuration for the rendering
     * @return the rendered [BufferedImage]
     */
    fun render(config: Config): BufferedImage {
        val image2D = Image2D(0).dimension(config.width, config.height).format(Format.RGBA8)
        logger.info { "Rendering Mandelbrot set with $config" }
        val start = TimeSource.Monotonic.markNow()
        val bufferedImage = compiledShader.dispatch(
            (config.width + LOCAL_SIZE - 1) / LOCAL_SIZE,
            (config.height + LOCAL_SIZE - 1) / LOCAL_SIZE,
            NamedUniform<Int>("maxIterations").value(config.maxIterations),
            NamedUniform<Double>("centerX").value(config.centerX),
            NamedUniform<Double>("centerY").value(config.centerY),
            NamedUniform<Double>("zoom").value(config.zoom),
            image2D
        ).use { it[image2D] }.toBufferedImage()!!
        logger.info { "Rendering done in ${start.elapsedNow()}" }
        return bufferedImage
    }

    override fun close() {
        compiledShader.close()
        super.close()
    }

    /**
     * Represents the configuration for the Mandelbrot set rendering.
     *
     * @property width the width of the rendered image
     * @property height the height of the rendered image
     * @property maxIterations the maximum number of iterations to perform
     * @property centerX the x-coordinate of the center of the rendered Mandelbrot set
     * @property centerY the y-coordinate of the center of the rendered Mandelbrot set
     * @property zoom the zoom factor to apply to the rendered Mandelbrot set
     */
    data class Config(
        val width: Int = 800,
        val height: Int = 600,
        val maxIterations: Int = 256,
        val centerX: Double = -0.5,
        val centerY: Double = 0.0,
        val zoom: Double = 1.0,
    )

    companion object {
        private const val SHADER_FILE = "mandelbrot.glsl"
        private const val LOCAL_SIZE = 8
        private val logger = KotlinLogging.logger {}
    }
}
