package de.hauschild.kompute.benchmark

import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

@State(Scope.Benchmark)
open class CpuState {
    @Setup(Level.Trial)
    fun setup() {
        CpuInfo.logCpuInfo()
    }
}
