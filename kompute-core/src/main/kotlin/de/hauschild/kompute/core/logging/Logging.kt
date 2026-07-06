/**
 * Logging utilities shared across backend and shader implementations.
 */

package de.hauschild.kompute.core.logging

import io.github.oshai.kotlinlogging.KLogger
import kotlin.time.Duration
import kotlin.time.measureTimedValue

/**
 * Executes [block], measures its duration, and logs the outcome at DEBUG level.
 *
 * Produces exactly one DEBUG log entry per operation instead of separate "starting"/"finished"
 * lines, while still allowing duration-based analysis of the log output.
 *
 * @param message builds the DEBUG log message from the measured [Duration]
 * @param block the operation to execute and time
 * @return the result of [block]
 */
inline fun <T> KLogger.debugTimed(crossinline message: (Duration) -> String, block: () -> T): T {
    val (result, duration) = measureTimedValue(block)
    debug { message(duration) }
    return result
}
