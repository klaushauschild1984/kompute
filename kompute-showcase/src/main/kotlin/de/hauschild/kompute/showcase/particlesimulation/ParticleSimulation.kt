package de.hauschild.kompute.showcase.particlesimulation

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
import java.lang.Math.random
import kotlin.math.ceil

/**
 * @param backend
 * @param closeBackend
 */
class ParticleSimulation(
    backend: Backend = Kompute.openGL(),
    closeBackend: Boolean = true,
) : BackendUser(backend, closeBackend) {
    var activeCount: Int = 0
        private set

    private val spawnShader = backend.shader(
        Code(checkNotNull(
            ParticleSimulation::class.java.getResourceAsStream(SPAWN_SHADER_FILE)
        ) {
            "Shader $SPAWN_SHADER_FILE not found"
        }.use { it.reader().readText().replace("\$LOCAL_SIZE", LOCAL_SIZE.toString()) })).compile()
    private val renderShader = backend.shader(
        Code(checkNotNull(
            ParticleSimulation::class.java.getResourceAsStream(RENDER_SHADER_FILE)
        ) {
            "Shader $RENDER_SHADER_FILE not found"
        }.use { it.reader().readText().replace("\$LOCAL_SIZE", LOCAL_SIZE.toString()) })).compile()
    private val updateShader = backend.shader(
        Code(checkNotNull(
            ParticleSimulation::class.java.getResourceAsStream(UPDATE_SHADER_FILE)
        ) {
            "Shader $UPDATE_SHADER_FILE not found"
        }.use { it.reader().readText().replace("\$LOCAL_SIZE", LOCAL_SIZE.toString()) })).compile()
    private val particleBuffer = StorageBuffer<ByteArray>(0).size(MAX_PARTICLES * BYTES_PER_PARTICLE).asOutput()

    /**
     * @param dt
     * @return
     */
    fun step(dt: Float): BufferedImage {
        if (activeCount == 0) {
            return BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
        }
        val workgroups = ceil(activeCount / LOCAL_SIZE.toFloat()).toInt()
        val canvas = Image2D(0).dimension(WIDTH, HEIGHT).format(Image2D.Format.RGBA8)
        return Pipeline().execute(
            Stage(updateShader, workgroups, data = listOf(
                particleBuffer,
                NamedUniform("activeCount", Int::class).value(activeCount),
                NamedUniform("dt", Float::class).value(dt),
                NamedUniform("gravity", Float::class).value(GRAVITY),
                NamedUniform("maxAge", Float::class).value(MAX_AGE),
                NamedUniform("boundsWidth", Float::class).value(WIDTH.toFloat()),
                NamedUniform("boundsHeight", Float::class).value(HEIGHT.toFloat()),
            )),
            Stage(renderShader, workgroups, data = listOf(
                particleBuffer,
                canvas,
                NamedUniform("activeCount", Int::class).value(activeCount),
                NamedUniform("maxAge", Float::class).value(MAX_AGE),
            )),
        ).use { it[canvas].toBufferedImage()!! }
    }

    /**
     * @param x
     * @param y
     */
    fun spawn(x: Float, y: Float) {
        if (activeCount >= MAX_PARTICLES) {
            return
        }
        val count = minOf((SPAWN_PER_FRAME * random()).toInt(), MAX_PARTICLES - activeCount) + 1
        if (count == 0) {
            return
        }
        val workgroups = ceil(count / LOCAL_SIZE.toFloat()).toInt()
        spawnShader.dispatch(
            workgroups,
            particleBuffer,
            NamedUniform("activeCount", Int::class).value(activeCount),
            NamedUniform("spawnCount", Int::class).value(count),
            NamedUniform("spawnX", Float::class).value(x),
            NamedUniform("spawnY", Float::class).value(y),
            NamedUniform("seed", Int::class).value(System.nanoTime().toInt()),
        ).close()
        activeCount += count
    }

    companion object {
        const val WIDTH = 400
        const val HEIGHT = 300
        const val LOCAL_SIZE = 64
        const val SPAWN_PER_FRAME = 1
        const val MAX_PARTICLES = 1_000_000
        const val BYTES_PER_PARTICLE = 32
        const val GRAVITY = 0.0f
        const val MAX_AGE = 10.0f
        const val SPAWN_SHADER_FILE = "particle-spawn.glsl"
        const val RENDER_SHADER_FILE = "particle-render.glsl"
        const val UPDATE_SHADER_FILE = "particle-update.glsl"
    }
}
