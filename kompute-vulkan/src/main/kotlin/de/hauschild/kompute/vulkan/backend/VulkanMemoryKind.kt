package de.hauschild.kompute.vulkan.backend

import org.lwjgl.util.vma.Vma

/**
 * Describes where a [VulkanAllocatedBuffer] should live, translated into VMA allocation flags.
 *
 * @property allocationFlags the `VmaAllocationCreateFlags` to request
 */
internal enum class VulkanMemoryKind(
    val allocationFlags: Int,
) {
    /**
     * Device-local memory, not accessible from the host.
     */
    DEVICE(0),

    /**
     * Persistently mapped host memory, optimized for sequential writes from the host (upload).
     */
    STAGING_UPLOAD(
        Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT or Vma.VMA_ALLOCATION_CREATE_MAPPED_BIT,
    ),

    /**
     * Persistently mapped host memory, optimized for random reads from the host (readback).
     */
    STAGING_READBACK(
        Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_RANDOM_BIT or Vma.VMA_ALLOCATION_CREATE_MAPPED_BIT,
    ),
    ;
}
