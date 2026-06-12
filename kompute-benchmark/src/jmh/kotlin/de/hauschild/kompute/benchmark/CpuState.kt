package de.hauschild.kompute.jmh.kotlin.de.hauschild.kompute.benchmark

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import oshi.SystemInfo

/**
 * Benchmark state logging CPU info.
 */
@State(Scope.Benchmark)
open class CpuState {
    private val logger = KotlinLogging.logger {}

    /**
     * Set up the state.
     */
    @Setup(Level.Trial)
    fun setup() {
        val systemInfo = SystemInfo()
        val cpu = systemInfo.hardware.processor
        logger.info {
            "CPU Info: ${cpu.processorIdentifier.name}, ${cpu.physicalProcessorCount} physical cores, ${cpu
                .logicalProcessorCount} logical cores, max freq: ${cpu.maxFreq / 1_000_000_000.0} GHz"
        }
    }
}
