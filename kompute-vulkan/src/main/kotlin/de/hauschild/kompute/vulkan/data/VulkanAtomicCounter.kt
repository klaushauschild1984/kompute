package de.hauschild.kompute.vulkan.data

import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.vulkan.VulkanBuffer
import de.hauschild.kompute.vulkan.VulkanReadable

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
 */
class VulkanAtomicCounter(
    source: AtomicCounter,
) : VulkanBuffer<AtomicCounter>(source),
OutputCapable<Int> by source,
VulkanReadable<Int> {
    private val wrappedSource = StorageBuffer(source.index, IntArray::class).asOutput()
    private val wrapped = VulkanStorageBuffer(wrappedSource)

    override fun bind() {
        wrappedSource.data(intArrayOf(source.start))
        wrapped.bind()
    }

    override fun read(): Int = wrapped.read()[0]

    override fun close() = wrapped.close()
}
