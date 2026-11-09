package de.hauschild.kompute.vulkan.backend

import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.vma.Vma
import org.lwjgl.vulkan.VK10
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A `VkBuffer` together with its VMA allocation.
 *
 * Instances are created by [VulkanAllocator.createBuffer]. [close] is idempotent.
 *
 * @property allocator the raw VMA allocator handle this buffer was created from
 * @property buffer the raw `VkBuffer` handle (non-dispatchable, hence a plain [Long])
 * @property allocation the raw VMA allocation handle
 * @property size the buffer size in bytes
 * @property mappedAddress the address of the persistently mapped memory, or `NULL` if not host-visible
 */
internal class VulkanAllocatedBuffer(
    val allocator: Long,
    val buffer: Long,
    val allocation: Long,
    val size: Long,
    val mappedAddress: Long,
) : AutoCloseable {
    /**
     * Whether the buffer has been destroyed.
     */
    var isClosed = false
        private set

    /**
     * Returns the persistently mapped memory of this buffer in native byte order.
     *
     * @return the mapped memory as a [ByteBuffer] spanning the whole buffer
     * @throws IllegalStateException if the buffer was not allocated as host-visible
     */
    fun mapped(): ByteBuffer {
        check(mappedAddress != MemoryUtil.NULL) { "Buffer is not host-visible" }
        return MemoryUtil.memByteBuffer(mappedAddress, size.toInt()).order(ByteOrder.nativeOrder())
    }

    /**
     * Makes host writes visible to the device. Must be called after writing to [mapped].
     */
    fun flush() {
        Vma.vmaFlushAllocation(allocator, allocation, 0, VK10.VK_WHOLE_SIZE)
    }

    /**
     * Makes device writes visible to the host. Must be called before reading from [mapped].
     */
    fun invalidate() {
        Vma.vmaInvalidateAllocation(allocator, allocation, 0, VK10.VK_WHOLE_SIZE)
    }

    override fun close() {
        if (!isClosed) {
            isClosed = true
            Vma.vmaDestroyBuffer(allocator, buffer, allocation)
        }
    }
}
