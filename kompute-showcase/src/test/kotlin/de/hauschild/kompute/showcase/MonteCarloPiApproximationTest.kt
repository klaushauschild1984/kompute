package de.hauschild.kompute.showcase

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.test.assertTrue

class MonteCarloPiApproximationTest {
    @ParameterizedTest
    @MethodSource
    fun `pi approximation accuracy`(samples: Int, expectedDigits: Int) {
        MonteCarloPiApproximation(samples = samples).use { monteCarloPiApproximation ->
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
