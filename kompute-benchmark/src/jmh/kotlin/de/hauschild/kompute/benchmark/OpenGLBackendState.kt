package de.hauschild.kompute.benchmark

import de.hauschild.kompute.core.Backend
import de.hauschild.kompute.core.Kompute
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown

/**
 * Benchmark state initializing and providing the OpenGL backend.
 */
@State(Scope.Benchmark)
open class OpenGLBackendState {
    /**
     * Initialized OpenGL backend.
     */
    lateinit var backend: Backend

    /**
     * Set up the state.
     */
    @Setup(Level.Trial)
    fun setup() {
        backend = Kompute.openGL()
    }

    /**
     * Tear down the state.
     */
    @TearDown(Level.Trial)
    fun tearDown() {
        backend.close()
    }
}
