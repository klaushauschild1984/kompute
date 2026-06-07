package de.hauschild.kompute.showcase

import de.hauschild.kompute.core.Kompute
import de.hauschild.kompute.core.backend.Backend
import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.execution.ShaderSource.Code

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
 * @param backend the compute backend to use — defaults to the OpenGL backend
 */
class MonteCarloPiApproximation(
    private val samples: Int = 1_000_000,
    private val backend: Backend = Kompute.openGL()
) : AutoCloseable by backend{
    private val shaderCode: String = checkNotNull(
        MonteCarloPiApproximation::class.java
            .getResourceAsStream("monte-carlo-pi-approximation.glsl")
    ) {
        "Shader monte-carlo-pi-approximation.glsl not found"
    }.use { it.reader().readText().replace("\$LOCAL_SIZE", LOCAL_SIZE.toString()) }

    /**
     * Runs the Monte Carlo simulation and returns the approximated value of π.
     *
     * @return the approximated value of π
     */
    fun approximate(): Double {
        val totalThreads = ((samples + LOCAL_SIZE - 1) / LOCAL_SIZE) * LOCAL_SIZE
        val workgroups = totalThreads / LOCAL_SIZE

        val hits = AtomicCounter(0)
        val result = backend
            .shader(
                Code(shaderCode))
            .data(hits)
            .dispatch(workgroups)
            .execute()

        return 4.0 * result[hits] / totalThreads
    }

    companion object {
        private const val LOCAL_SIZE = 64
    }
}
