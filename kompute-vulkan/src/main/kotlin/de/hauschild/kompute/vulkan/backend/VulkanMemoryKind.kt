package de.hauschild.kompute.vulkan.backend

import org.lwjgl.util.vma.Vma

private const val RANDOM_ACCESS_OR_TRANSFER =
    Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_RANDOM_BIT or Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT
private const val SHARED_FLAGS = RANDOM_ACCESS_OR_TRANSFER or Vma.VMA_ALLOCATION_CREATE_MAPPED_BIT

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
     * Device-local memory that is also persistently mapped if the device has a memory type that is
     * device-local and host-visible (unified memory, e.g. integrated GPUs). Otherwise it is plain
     * device-local memory, then data has to be moved through staging buffers.
     */
    DEVICE_PREFER_SHARED(SHARED_FLAGS),

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
