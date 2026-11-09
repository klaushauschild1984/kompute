package de.hauschild.kompute.vulkan.backend

/**
 * The hardware limits of the selected physical device that are validated before work is submitted.
 *
 * Vulkan reports these as unsigned 32 bit values, they are widened to [Long] so that values above
 * [Int.MAX_VALUE] stay positive.
 *
 * @property maxComputeWorkGroupCountX maximum number of work groups in the X dimension of a dispatch
 * @property maxComputeWorkGroupCountY maximum number of work groups in the Y dimension of a dispatch
 * @property maxComputeWorkGroupCountZ maximum number of work groups in the Z dimension of a dispatch
 * @property maxImageDimension2D maximum width and height of a 2D image
 * @property maxStorageBufferRange maximum size in bytes of a storage buffer descriptor
 * @property maxUniformBufferRange maximum size in bytes of a uniform buffer descriptor
 */
data class VulkanLimits(
    val maxComputeWorkGroupCountX: Long,
    val maxComputeWorkGroupCountY: Long,
    val maxComputeWorkGroupCountZ: Long,
    val maxImageDimension2D: Long,
    val maxStorageBufferRange: Long,
    val maxUniformBufferRange: Long,
)
