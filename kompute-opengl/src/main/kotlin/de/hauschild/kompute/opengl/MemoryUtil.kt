/**
 * LWJGL memory utility extensions for safe native buffer lifecycle management.
 */

package de.hauschild.kompute.opengl

import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * Executes [block] with this [ByteBuffer] and frees the native memory afterwards.
 *
 * @param block the operation to perform with this buffer
 * @return the result of [block]
 */
internal inline fun <R> ByteBuffer.useAndFree(block: (ByteBuffer) -> R): R =
    try {
        block(this)
    } finally {
        MemoryUtil.memFree(this)
    }
