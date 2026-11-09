package de.hauschild.kompute.vulkan.backend

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkCommandBuffer
import java.nio.ByteBuffer

/**
 * Collects the host <-> device copies that belong to one dispatch, so that they share a single
 * submission with the dispatch itself: the uploads run before it, the readbacks after it.
 *
 * Without a batch every copy would be a submission of its own, and a submission - recording, queue
 * submit and waiting for the fence - is the most expensive part of a small dispatch.
 *
 * Buffers that the host can access directly (see [VulkanAllocatedBuffer.isDirectlyAccessible]) need
 * no copy at all: uploads are written to them right away and downloads read them in place, after the
 * dispatch made its writes visible to the host.
 *
 * The staging buffers of the batch are released on [close]. If a submission times out they are kept,
 * because the GPU may still use them, and released with the backend.
 *
 * @param context the [VulkanContext] providing allocator and queue
 */
class VulkanTransferBatch(
    private val context: VulkanContext,
) : AutoCloseable {
    private val stagingBuffers = mutableListOf<VulkanAllocatedBuffer>()
    private val uploads = mutableListOf<(VkCommandBuffer) -> Unit>()
    private val downloads = mutableListOf<(VkCommandBuffer) -> Unit>()
    private var abandoned = false
    private var hostReadsPending = false

    /**
     * Fills [target] with the bytes written by [write]: right away if the host can access the buffer
     * directly, otherwise by scheduling a copy from a staging buffer.
     *
     * @param target the device-local buffer, must have been created with `TRANSFER_DST` usage
     * @param write writes up to [VulkanAllocatedBuffer.size] bytes into the given memory
     */
    internal fun upload(
        target: VulkanAllocatedBuffer,
        write: (ByteBuffer) -> Unit,
    ) {
        if (target.isDirectlyAccessible) {
            write(target.mapped())
            target.flush()
            return
        }
        val staging = createStaging(
            target.size, VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VulkanMemoryKind.STAGING_UPLOAD,
        )
        write(staging.mapped())
        staging.flush()
        uploads += { commandBuffer -> commandBuffer.copyBuffer(staging.buffer, target.buffer, target.size) }
    }

    /**
     * Schedules a command that writes to a resource through the transfer stage, e.g. clearing an
     * image. Its writes become visible to the compute shader like those of an [upload].
     *
     * @param command records the command
     */
    internal fun recordTransfer(command: (VkCommandBuffer) -> Unit) {
        uploads += command
    }

    /**
     * Notes that the dispatch uses [buffer], so that the writes of the shader are made visible to the
     * host if the host reads the buffer directly.
     *
     * @param buffer a buffer bound for the dispatch
     */
    internal fun use(buffer: VulkanAllocatedBuffer) {
        if (buffer.isDirectlyAccessible) {
            hostReadsPending = true
        }
    }

    /**
     * Schedules copying [source] to host memory, which is not necessary if the host can access the
     * buffer directly.
     *
     * @param source the device-local buffer, must have been created with `TRANSFER_SRC` usage
     * @return the pending download, readable after [submit]
     */
    internal fun download(source: VulkanAllocatedBuffer): VulkanDownload {
        if (source.isDirectlyAccessible) {
            return VulkanDownload(source)
        }
        val staging = createStaging(
            source.size, VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT, VulkanMemoryKind.STAGING_READBACK,
        )
        downloads += { commandBuffer -> commandBuffer.copyBuffer(source.buffer, staging.buffer, source.size) }
        return VulkanDownload(staging)
    }

    /**
     * Schedules copying a tightly packed 2D image in `VK_IMAGE_LAYOUT_GENERAL` to host memory.
     *
     * @param image the image, must have been created with `TRANSFER_SRC` usage
     * @param width the image width in texels
     * @param height the image height in texels
     * @param byteSize the size of the whole image in bytes
     * @return the pending download, readable after [submit]
     */
    internal fun downloadImage(
        image: VulkanAllocatedImage,
        width: Int,
        height: Int,
        byteSize: Long,
    ): VulkanDownload {
        val staging = createStaging(
            byteSize, VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT, VulkanMemoryKind.STAGING_READBACK,
        )
        downloads += { commandBuffer -> commandBuffer.copyImageToBuffer(image.image, staging.buffer, width, height) }
        return VulkanDownload(staging)
    }

    /**
     * Submits everything that was scheduled in one command buffer and waits for it: first the
     * uploads, then [additional] (typically the dispatch), then the downloads. Barriers separate the
     * three. Nothing is submitted if there is nothing to do.
     *
     * @param additional records further commands between the uploads and the downloads
     * @throws VulkanTimeoutException if the GPU did not finish within the timeout
     */
    fun submit(additional: ((VkCommandBuffer) -> Unit)? = null) {
        if (uploads.isEmpty() && downloads.isEmpty() && additional == null) {
            return
        }
        try {
            context.commandPool.executeOneTime { commandBuffer ->
                record(commandBuffer, additional)
            }
        } catch (timeout: VulkanTimeoutException) {
            abandon()
            throw timeout
        }
        uploads.clear()
        downloads.clear()
        hostReadsPending = false
    }

    override fun close() {
        if (!abandoned) {
            stagingBuffers.forEach { it.close() }
        }
        stagingBuffers.clear()
    }

    private fun record(
        commandBuffer: VkCommandBuffer,
        additional: ((VkCommandBuffer) -> Unit)?,
    ) {
        uploads.forEach { it(commandBuffer) }
        if (uploads.isNotEmpty()) {
            commandBuffer.memoryBarrier(
                VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK10.VK_ACCESS_TRANSFER_WRITE_BIT,
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_ACCESS_SHADER_READ_BIT or VK10.VK_ACCESS_SHADER_WRITE_BIT,
            )
        }
        additional?.invoke(commandBuffer)
        if (hostReadsPending) {
            commandBuffer.memoryBarrier(
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_ACCESS_SHADER_WRITE_BIT,
                VK10.VK_PIPELINE_STAGE_HOST_BIT,
                VK10.VK_ACCESS_HOST_READ_BIT,
            )
        }
        if (downloads.isNotEmpty()) {
            commandBuffer.memoryBarrier(
                VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK10.VK_ACCESS_SHADER_WRITE_BIT,
                VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK10.VK_ACCESS_TRANSFER_READ_BIT,
            )
            downloads.forEach { it(commandBuffer) }
            commandBuffer.memoryBarrier(
                VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK10.VK_ACCESS_TRANSFER_WRITE_BIT,
                VK10.VK_PIPELINE_STAGE_HOST_BIT,
                VK10.VK_ACCESS_HOST_READ_BIT,
            )
        }
    }

    private fun createStaging(
        size: Long,
        usage: Int,
        kind: VulkanMemoryKind,
    ): VulkanAllocatedBuffer = context.allocator.createBuffer(size, usage, kind).also { stagingBuffers += it }

    private fun abandon() {
        abandoned = true
        val kept = stagingBuffers.toList()
        context.resources.register { kept.forEach { it.close() } }
    }
}
