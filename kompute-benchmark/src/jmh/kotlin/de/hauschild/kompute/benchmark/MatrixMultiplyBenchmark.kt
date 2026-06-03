package de.hauschild.kompute.benchmark

import de.hauschild.kompute.core.Backend
import de.hauschild.kompute.core.Kompute
import de.hauschild.kompute.core.ShaderSource.Stream
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
open class MatrixMultiplyBenchmark {
    @Param("128", "512", "1024")
    var size: Int = 0

    private lateinit var a: FloatArray
    private lateinit var b: FloatArray
    private lateinit var c: FloatArray
    private lateinit var openGLBackend: Backend

    @Setup
    fun setup() {
        a = FloatArray(size * size) { it.toFloat() }
        b = FloatArray(size * size) { it.toFloat() }
        c = FloatArray(size * size)
        openGLBackend = Kompute.openGL()
    }

    @Benchmark
    fun kotlin(): FloatArray {
        val result = FloatArray(size * size)
        for (i in 0 until size) {
            for (j in 0 until size) {
                var sum = 0.0f
                for (k in 0 until size) {
                    sum += a[i * size + k] * b[k * size + j]
                }
                result[i * size + j] = sum
            }
        }
        return result
    }

    @Suppress("standard:chain-wrapping")
    @Benchmark
    fun openGL(): FloatArray =
        openGLBackend
            .shader(
                Stream(
                    MatrixMultiplyBenchmark::class.java
                        .getResourceAsStream("/matrix_multiply.glsl")!!,
                ),
            ).input(0)
            .buffer(a)
            .input(1)
            .buffer(b)
            .output(2, "c")
            .buffer(c)
            .dispatch(size / 8, size / 8)
            .execute()
            .output("c")
}
