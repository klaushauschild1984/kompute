package de.hauschild.kompute.showcase

import de.hauschild.kompute.showcase.MandelbrotRenderer.Config
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.awt.Desktop
import java.io.File
import javax.imageio.ImageIO

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MandelbrotRendererTest {
    private val renderer = MandelbrotRenderer()

    @AfterAll
    fun tearDown() = renderer.close()

    @ParameterizedTest
    @MethodSource("configs")
    fun render(namedConfig: NamedConfig) {
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

    data class NamedConfig(val name: String, val config: Config)

    companion object {
        @JvmStatic
        fun configs() = listOf(
            Arguments.of(NamedConfig("default", Config())),
        )
    }
}
