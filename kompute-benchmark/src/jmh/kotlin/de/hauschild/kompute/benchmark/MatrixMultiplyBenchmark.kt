package de.hauschild.kompute.jmh.kotlin.de.hauschild.kompute.benchmark

import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.shader.CompiledShader
import de.hauschild.kompute.core.shader.ShaderSource.Glsl.Stream
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
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Warmup

import java.util.concurrent.TimeUnit

/**
 * Benchmark for matrix multiplication.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
open class MatrixMultiplyBenchmark {
    /**
     * Kotlin benchmark.
     *
     * @param matrix
     * @param cpu
     */
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

    /**
     * OpenGL benchmark.
     *
     * @param matrix
     * @param openGLShader
     */
    @Benchmark
    fun openGL(
        matrix: MatrixState,
        openGLShader: OpenGLShaderState,
    ) {
        openGLShader.shader.dispatch(
            matrix.size / 8, matrix.size / 8,
            StorageBuffer<FloatArray>(0).data(matrix.a),
            StorageBuffer<FloatArray>(1).data(matrix.b),
            StorageBuffer<FloatArray>(2).size(matrix.size * matrix.size).asOutput(),
        ).close()
    }

    /**
     * Benchmark state holding the compiled matrix-multiply shader.
     */
    @State(Scope.Benchmark)
    open class OpenGLShaderState {
        /**
         * Compiled matrix-multiply shader.
         */
        lateinit var shader: CompiledShader

        /**
         * Compiles the shader once per trial.
         *
         * @param openGLBackend
         */
        @Setup(Level.Trial)
        fun setup(openGLBackend: OpenGLBackendState) {
            shader = openGLBackend.backend
                .shader(
                    Stream(
                        MatrixMultiplyBenchmark::class.java
                            .getResourceAsStream("matrix-multiply.glsl")!!,
                    ),
                )
                .compile()
        }

        /**
         * Releases the compiled shader after the trial.
         */
        @TearDown(Level.Trial)
        fun tearDown() {
            shader.close()
        }
    }

    /**
     * Benchmark state for matrix multiplication.
     */
    @State(Scope.Benchmark)
    open class MatrixState {
        /**
         * Matrix size.
         */
        @Param(
            "128",
            "512",
            "1024"
        )
        var size: Int = 0

        /**
         * Matrix A.
         */
        lateinit var a: FloatArray

        /**
         * Matrix B.
         */
        lateinit var b: FloatArray

        /**
         * Set up the state.
         */
        @Setup(Level.Trial)
        fun setup() {
            a = FloatArray(size * size) { it.toFloat() }
            b = FloatArray(size * size) { it.toFloat() }
        }
    }
}
