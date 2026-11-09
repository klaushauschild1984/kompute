package de.hauschild.kompute.vulkan.backend

import java.nio.ByteBuffer

/**
 * A readback that has been recorded in a [VulkanTransferBatch]: the bytes can be read once the batch
 * has been submitted, and only as long as the batch is open. For a buffer the host can access
 * directly it reads the buffer itself.
 *
 * @param staging the host-visible buffer the data is copied to, or the buffer itself if it is directly accessible
 */
internal class VulkanDownload(
    private val staging: VulkanAllocatedBuffer,
) {
    /**
     * Returns the downloaded bytes in native byte order.
     *
     * @return the bytes, only valid until the owning batch is closed
     */
    fun bytes(): ByteBuffer {
        staging.invalidate()
        return staging.mapped()
    }
}
