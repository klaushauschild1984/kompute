package de.hauschild.kompute.showcase

import de.hauschild.kompute.core.backend.Backend
import de.hauschild.kompute.opengl.OpenGLBackendExtension
import de.hauschild.kompute.showcase.MandelbrotRenderer.Config
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.awt.Desktop
import java.io.File
import javax.imageio.ImageIO

@ExtendWith(OpenGLBackendExtension::class)
class MandelbrotRendererTest {
    @ParameterizedTest
    @MethodSource("configs")
    fun render(namedConfig: NamedConfig, backend: Backend) {
        MandelbrotRenderer(backend = backend, closeBackend = false).use { renderer ->
            val image = renderer.render(namedConfig.config)
            val outputFile = File("build/test-output/mandelbrot-${namedConfig.name}.png")
            outputFile.parentFile.mkdirs()
            ImageIO.write(image, "PNG", outputFile)
            if (Desktop.isDesktopSupported()) {
                runCatching {
                    Desktop.getDesktop().open(outputFile)
                }
            }
        }
    }

    data class NamedConfig(val name: String, val config: Config)

    companion object {
        @JvmStatic
        fun configs() = listOf(
            Arguments.of(NamedConfig("default", Config())),
        )
    }
}
