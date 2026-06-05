package de.hauschild.kompute.benchmark

import de.hauschild.kompute.core.ShaderSource.Stream
import de.hauschild.kompute.core.StorageBuffer
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
open class MatrixMultiplyBenchmark {
    @Benchmark
    fun kotlin(
        matrix: MatrixState,
        cpu: CpuState,
    ) {
        val result = FloatArray(matrix.size * matrix.size)
        for (i in 0 until matrix.size) {
            for (j in 0 until matrix.size) {
                var sum = 0.0f
                for (k in 0 until matrix.size) {
                    sum += matrix.a[i * matrix.size + k] * matrix.b[k * matrix.size + j]
                }
                result[i * matrix.size + j] = sum
            }
        }
    }

    @Benchmark
    fun openGL(
        matrix: MatrixState,
        openGLBackend: OpenGLBackendState,
    ) = openGLBackend.backend
        .shader(
            Stream(
                MatrixMultiplyBenchmark::class.java
                    .getResourceAsStream("matrix-multiply.glsl")!!,
            ),
        ).data(
            StorageBuffer<FloatArray>(0).data(matrix.a),
            StorageBuffer<FloatArray>(1).data(matrix.b),
            StorageBuffer<FloatArray>(2).size(matrix.size * matrix.size).asOutput(),
        ).dispatch(matrix.size / 8, matrix.size / 8)
        .execute()

    @State(Scope.Benchmark)
    open class MatrixState {
        @Param("128", "512", "1024")
        var size: Int = 0

        lateinit var a: FloatArray
        lateinit var b: FloatArray

        @Setup(Level.Trial)
        fun setup() {
            a = FloatArray(size * size) { it.toFloat() }
            b = FloatArray(size * size) { it.toFloat() }
        }
    }
}
