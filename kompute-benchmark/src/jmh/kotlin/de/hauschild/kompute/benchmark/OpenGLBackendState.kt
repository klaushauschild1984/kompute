package de.hauschild.kompute.benchmark

import de.hauschild.kompute.core.Backend
import de.hauschild.kompute.core.Kompute
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown

@State(Scope.Benchmark)
open class OpenGLBackendState {
    lateinit var backend: Backend

    @Setup(Level.Trial)
    fun setup() {
        backend = Kompute.openGL()
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        backend.close()
    }
}
