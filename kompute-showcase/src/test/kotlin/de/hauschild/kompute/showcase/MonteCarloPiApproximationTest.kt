package de.hauschild.kompute.showcase

import de.hauschild.kompute.core.backend.Backend
import de.hauschild.kompute.opengl.OpenGLBackendExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.test.assertTrue

@ExtendWith(OpenGLBackendExtension::class)
class MonteCarloPiApproximationTest {
    @ParameterizedTest
    @MethodSource
    fun `pi approximation accuracy`(
        samples: Int,
        expectedDigits: Int,
        backend: Backend
    ) {
        MonteCarloPiApproximation(samples = samples, backend = backend, closeBackend = false)
            .use { monteCarloPiApproximation ->
                val pi = monteCarloPiApproximation.approximate()
                val error = abs(pi - Math.PI)
                val correctDigits = floor(-log10(error)).toInt()
                assertTrue(correctDigits >= expectedDigits)
            }
    }

    companion object {
        @JvmStatic
        fun `pi approximation accuracy`() = listOf(
            Arguments.of(10_000, 1),
            Arguments.of(100_000, 2),
            Arguments.of(1_000_000, 3),
        )
    }
}
