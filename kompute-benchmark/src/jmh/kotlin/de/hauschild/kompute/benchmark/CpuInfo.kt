package de.hauschild.kompute.benchmark

import io.github.oshai.kotlinlogging.KotlinLogging
import oshi.SystemInfo

object CpuInfo {
    private val logger = KotlinLogging.logger {}

    fun logCpuInfo() {
        val si = SystemInfo()
        val cpu = si.hardware.processor
        logger.info {
            "CPU Info: ${cpu.processorIdentifier.name}, ${cpu.physicalProcessorCount} physical cores, ${cpu.logicalProcessorCount} logical cores, max freq: ${cpu.maxFreq / 1_000_000_000.0} GHz"
        }
    }
}
