package de.hauschild.kompute.showcase

import de.hauschild.kompute.core.Kompute
import de.hauschild.kompute.core.backend.Backend
import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.shader.ShaderSource.Code

/**
 * Approximates π using the Monte Carlo method on the GPU.
 *
 * Each GPU thread generates a random point (x, y) in the unit square [0, 1] × [0, 1]
 * and checks whether it falls inside the unit circle (x² + y² ≤ 1). The ratio of hits
 * to total samples converges to π/4, so π ≈ 4 × hits / samples.
 *
 * Accuracy grows with √samples — roughly one additional correct decimal place per
 * two orders of magnitude more samples.
 *
 * ```kotlin
 * val pi = MonteCarloPiApproximation(samples = 1_000_000).approximate()
 * ```
 *
 * @param samples the total number of random points to sample — rounded up to the next
 * multiple of the internal workgroup size
 * @param backend the [Backend] to be used for rendering - defaults to [Kompute.openGL]
 * @param closeBackend whether to close the backend after use - defaults to true
 */
class MonteCarloPiApproximation(
    private val samples: Int = 1_000_000,
    backend: Backend = Kompute.openGL(),
    closeBackend: Boolean = true,
) : BackendUser(backend, closeBackend) {
    private val compiledShader = backend.shader(Code(checkNotNull(
        MonteCarloPiApproximation::class.java
            .getResourceAsStream(SHADER_FILE)
    ) {
        "Shader $SHADER_FILE not found"
    }.use { it.reader().readText().replace("\$LOCAL_SIZE", LOCAL_SIZE.toString()) }
    )).compile()

    /**
     * Runs the Monte Carlo simulation and returns the approximated value of π.
     *
     * @return the approximated value of π
     */
    fun approximate(): Double {
        val totalThreads = ((samples + LOCAL_SIZE - 1) / LOCAL_SIZE) * LOCAL_SIZE
        val workgroups = totalThreads / LOCAL_SIZE

        val hits = AtomicCounter(0)
        val result = compiledShader.dispatch(workgroups, hits)

        return 4.0 * result[hits] / totalThreads
    }

    override fun close() {
        compiledShader.close()
        super.close()
    }

    companion object {
        private const val LOCAL_SIZE = 64
        private const val SHADER_FILE = "monte-carlo-pi-approximation.glsl"
    }
}
