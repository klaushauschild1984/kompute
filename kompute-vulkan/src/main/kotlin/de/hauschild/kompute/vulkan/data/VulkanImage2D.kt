package de.hauschild.kompute.vulkan.data

import de.hauschild.kompute.core.data.Image2D
import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.exception.KomputeBackendInitializationException
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import de.hauschild.kompute.core.exception.requireConfiguration
import de.hauschild.kompute.vulkan.VulkanBuffer
import de.hauschild.kompute.vulkan.VulkanReadable
import de.hauschild.kompute.vulkan.backend.VulkanAllocatedImage
import de.hauschild.kompute.vulkan.backend.VulkanContext
import de.hauschild.kompute.vulkan.backend.VulkanTransferBatch
import de.hauschild.kompute.vulkan.backend.checkVk
import de.hauschild.kompute.vulkan.backend.imageBarrier
import de.hauschild.kompute.vulkan.backend.submitRead
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkClearColorValue
import org.lwjgl.vulkan.VkDescriptorImageInfo
import org.lwjgl.vulkan.VkImageSubresourceRange
import org.lwjgl.vulkan.VkImageViewCreateInfo
import org.lwjgl.vulkan.VkWriteDescriptorSet

/**
 * Wraps a Vulkan storage image for a [Image2D].
 *
 * The image lives in device-local memory in `VK_IMAGE_LAYOUT_GENERAL` and is cleared to zero in
 * [bind], like the OpenGL backend does. [read] copies it to host memory through a temporary staging
 * buffer, the bytes are tightly packed with the first row first.
 *
 * Note: the shader has to declare a matching format, e.g. `layout(rgba8, binding = 0)` for
 * [Image2D.Format.RGBA8]. [Image2D.Format.R8] needs the `shaderStorageImageExtendedFormats` feature.
 *
 * @param source the [Image2D] configuration this image is based on
 * @param context the [VulkanContext] used for memory allocation and transfer commands
 */
class VulkanImage2D(
    source: Image2D,
    context: VulkanContext,
) : VulkanBuffer<Image2D>(source, context),
OutputCapable<Image2D.Image2DResult> by source,
VulkanReadable<Image2D.Image2DResult> {
    private var image: VulkanAllocatedImage? = null
    private var imageView: Long = VK10.VK_NULL_HANDLE

    /**
     * Allocates a new image and view for the configured dimension and format and schedules clearing
     * the image.
     *
     * Calling [bind] again releases the previous image and creates a new one.
     *
     * @throws KomputeConfigurationException if the dimension is not set, not positive or exceeds the device limit
     */
    override fun scheduleBind(batch: VulkanTransferBatch) {
        close()
        val width = checkNotNull(source.width) { "Image2D width must be set" }
        val height = checkNotNull(source.height) { "Image2D height must be set" }
        requireConfiguration(width > 0 && height > 0) { "Image2D dimension must be positive" }
        val maxDimension = context.limits.maxImageDimension2D
        requireConfiguration(width <= maxDimension && height <= maxDimension) {
            "Image2D size (${width}x$height) exceeds the device limit of $maxDimension"
        }

        val format = vkFormatOf(source.format)
        val newImage = context.allocator.createImage(width, height, format, IMAGE_USAGE)
        image = newImage
        imageView = createImageView(newImage, format)
        clear(batch, newImage)
    }

    /**
     * Copies the image back to host memory in a submission of its own.
     *
     * @return the pixels together with dimension and format
     * @throws KomputeBackendInitializationException if [bind] was not called before
     */
    override fun read(): Image2D.Image2DResult = context.submitRead { batch -> scheduleRead(batch) }

    override fun scheduleRead(batch: VulkanTransferBatch): () -> Image2D.Image2DResult {
        val boundImage = checkNotNull(image) { "bind() must be called before read()" }
        val width = checkNotNull(source.width) { "Image2D width must be set" }
        val height = checkNotNull(source.height) { "Image2D height must be set" }
        val byteSize = width.toLong() * height * source.format.bytesPerPixel
        val download = batch.downloadImage(boundImage, width, height, byteSize)
        return {
            val bytes = ByteArray(byteSize.toInt()).also { download.bytes().get(it) }
            Image2D.Image2DResult(bytes, width, height, source.format)
        }
    }

    override fun describe(
        write: VkWriteDescriptorSet,
        stack: MemoryStack,
    ) {
        check(imageView != VK10.VK_NULL_HANDLE) { "bind() must be called before describe()" }
        val imageInfo = VkDescriptorImageInfo.calloc(1, stack)
            .imageView(imageView)
            .imageLayout(VK10.VK_IMAGE_LAYOUT_GENERAL)
        write.dstBinding(source.index)
            .descriptorType(VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
            .descriptorCount(1)
            .pImageInfo(imageInfo)
    }

    override fun close() {
        if (imageView != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyImageView(context.device.device, imageView, null)
            imageView = VK10.VK_NULL_HANDLE
        }
        image?.close()
        image = null
    }

    private fun createImageView(
        boundImage: VulkanAllocatedImage,
        format: Int,
    ): Long = MemoryStack.stackPush().use { stack ->
        val createInfo = VkImageViewCreateInfo.calloc(stack)
            .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
            .image(boundImage.image)
            .viewType(VK10.VK_IMAGE_VIEW_TYPE_2D)
            .format(format)
        createInfo.subresourceRange()
            .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
            .baseMipLevel(0)
            .levelCount(1)
            .baseArrayLayer(0)
            .layerCount(1)
        val viewPointer = stack.mallocLong(1)
        checkVk(VK10.vkCreateImageView(context.device.device, createInfo, null, viewPointer)) {
            "Failed to create Vulkan image view"
        }
        viewPointer.get(0)
    }

    private fun clear(
        batch: VulkanTransferBatch,
        boundImage: VulkanAllocatedImage,
    ) {
        batch.recordTransfer { commandBuffer ->
            commandBuffer.imageBarrier(
                boundImage.image,
                VK10.VK_IMAGE_LAYOUT_UNDEFINED,
                VK10.VK_IMAGE_LAYOUT_GENERAL,
                VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                0,
                VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK10.VK_ACCESS_TRANSFER_WRITE_BIT,
            )
            MemoryStack.stackPush().use { stack ->
                val range = VkImageSubresourceRange.calloc(1, stack)
                    .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1)
                VK10.vkCmdClearColorImage(
                    commandBuffer,
                    boundImage.image,
                    VK10.VK_IMAGE_LAYOUT_GENERAL,
                    VkClearColorValue.calloc(stack),
                    range,
                )
            }
        }
    }

    private fun vkFormatOf(format: Image2D.Format): Int = when (format) {
        is Image2D.Format.RGBA8 -> VK10.VK_FORMAT_R8G8B8A8_UNORM
        is Image2D.Format.R8 -> VK10.VK_FORMAT_R8_UNORM
    }

    companion object {
        private const val TRANSFER_USAGE =
            VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT or VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT
        private const val IMAGE_USAGE = VK10.VK_IMAGE_USAGE_STORAGE_BIT or TRANSFER_USAGE
    }
}
