package de.hauschild.kompute.showcase.gameoflife

import de.hauschild.kompute.core.Kompute
import de.hauschild.kompute.core.backend.Backend
import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.data.NamedUniform
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.pipeline.Pipeline
import de.hauschild.kompute.core.pipeline.Stage
import de.hauschild.kompute.core.shader.ShaderSource.Code
import de.hauschild.kompute.showcase.BackendUser
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.random.Random

/**
 * @param backend
 * @param closeBackend
 */
class GameOfLife(
    backend: Backend = Kompute.openGL(),
    closeBackend: Boolean = true,
) : BackendUser(backend, closeBackend) {
    /**
     * The number of completed generations since the simulation started.
     */
    var generation: Int = 0
        private set
    private val updateShader = backend.shader(
        Code(checkNotNull(
            GameOfLife::class.java.getResourceAsStream(UPDATE_SHADER_FILE)
        ) { "Shader $UPDATE_SHADER_FILE not found" }
            .use { it.reader().readText().replace("\$LOCAL_SIZE", LOCAL_SIZE.toString()) })
    ).compile()
    private val renderShader = backend.shader(
        Code(checkNotNull(
            GameOfLife::class.java.getResourceAsStream(RENDER_SHADER_FILE)
        ) { "Shader $RENDER_SHADER_FILE not found" }
            .use { it.reader().readText().replace("\$LOCAL_SIZE", LOCAL_SIZE.toString()) })
    ).compile()
    private val cellBuffer = StorageBuffer<IntArray>(0)
        .data(randomState())
        .asOutput()
    private var readOffset = 0
    private var writeOffset = WIDTH * HEIGHT

    /**
     * Advances the simulation by one generation and returns the rendered frame.
     *
     * @return the rendered frame as a [BufferedImage]
     */
    fun step(): BufferedImage {
        val workgroups = ceil(WIDTH * HEIGHT / LOCAL_SIZE.toFloat()).toInt()
        val canvas = Image2D(0).dimension(WIDTH, HEIGHT).format(Image2D.Format.RGBA8)
        return Pipeline().execute(
            Stage(updateShader, workgroups, data = listOf(
                cellBuffer,
                NamedUniform("width", Int::class).value(WIDTH),
                NamedUniform("height", Int::class).value(HEIGHT),
                NamedUniform("readOffset", Int::class).value(readOffset),
                NamedUniform("writeOffset", Int::class).value(writeOffset),
            )),
            Stage(renderShader, workgroups, data = listOf(
                cellBuffer,
                canvas,
                NamedUniform("width", Int::class).value(WIDTH),
                NamedUniform("height", Int::class).value(HEIGHT),
                NamedUniform("displayOffset", Int::class).value(writeOffset),
            )),
        ).use { it[canvas].toBufferedImage()!! }.also {
            val tmp = readOffset
            readOffset = writeOffset
            writeOffset = tmp
            generation++
        }
    }

    override fun close() {
        updateShader.close()
        renderShader.close()
        super.close()
    }

    companion object {
        const val WIDTH = 256
        const val HEIGHT = 256
        const val LOCAL_SIZE = 64
        private const val UPDATE_SHADER_FILE = "gol-update.glsl"
        private const val RENDER_SHADER_FILE = "gol-render.glsl"

        private fun randomState(): IntArray {
            val cells = IntArray(2 * WIDTH * HEIGHT)
            for (i in 0 until WIDTH * HEIGHT) {
                cells[i] = if (Random.nextFloat() < 0.35f) 1 else 0
            }
            return cells
        }
    }
}
