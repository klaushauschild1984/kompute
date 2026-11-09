package de.hauschild.kompute.vulkan.data

import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.vulkan.VulkanBuffer
import de.hauschild.kompute.vulkan.VulkanReadable
import de.hauschild.kompute.vulkan.backend.VulkanContext
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkWriteDescriptorSet

/**
 * Wraps [AtomicCounter] as a thin adapter around a [VulkanStorageBuffer].
 *
 * Vulkan has no dedicated atomic-counter resource type - the shader-side equivalent is a regular
 * storage buffer with an atomic operation on one of its members (see `VULKAN-TODO.md`). This class
 * translates [AtomicCounter]'s single-`Int` contract to and from an internal
 * `StorageBuffer<IntArray>` of size 1, re-uploading [AtomicCounter.start] on every [bind] to match
 * the reset-per-dispatch semantics of the OpenGL backend's `OpenGLAtomicCounter`.
 *
 * @param source the [AtomicCounter] configuration this counter is based on
 * @param context the [VulkanContext] used for memory allocation and transfer commands
 */
class VulkanAtomicCounter(
    source: AtomicCounter,
    context: VulkanContext,
) : VulkanBuffer<AtomicCounter>(source),
OutputCapable<Int> by source,
VulkanReadable<Int> {
    private val wrappedSource = StorageBuffer(source.index, IntArray::class).asOutput()
    private val wrapped = VulkanStorageBuffer(wrappedSource, context)

    override fun bind() {
        wrappedSource.data(intArrayOf(source.start))
        wrapped.bind()
    }

    override fun read(): Int = wrapped.read()[0]

    override fun describe(
        write: VkWriteDescriptorSet,
        stack: MemoryStack,
    ) = wrapped.describe(write, stack)

    override fun close() = wrapped.close()
}
