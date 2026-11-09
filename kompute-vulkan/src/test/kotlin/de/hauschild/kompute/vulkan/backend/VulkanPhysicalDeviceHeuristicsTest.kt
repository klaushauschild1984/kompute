package de.hauschild.kompute.vulkan.backend

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.lwjgl.vulkan.VK10.VK_MEMORY_HEAP_DEVICE_LOCAL_BIT
import org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_CPU
import org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU
import org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU
import org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_OTHER
import org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU
import org.lwjgl.vulkan.VK10.VK_QUEUE_COMPUTE_BIT
import org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT
import org.lwjgl.vulkan.VK10.VK_QUEUE_TRANSFER_BIT

class VulkanPhysicalDeviceHeuristicsTest {
    @Test
    fun `discrete GPU ranks above integrated GPU`() {
        assertThat(deviceTypeRank(VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU))
            .isGreaterThan(deviceTypeRank(VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU))
    }

    @Test
    fun `integrated GPU ranks above virtual GPU`() {
        assertThat(deviceTypeRank(VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU))
            .isGreaterThan(deviceTypeRank(VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU))
    }

    @Test
    fun `virtual GPU ranks above CPU`() {
        assertThat(deviceTypeRank(VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU))
            .isGreaterThan(deviceTypeRank(VK_PHYSICAL_DEVICE_TYPE_CPU))
    }

    @Test
    fun `CPU ranks above an unrecognized device type`() {
        assertThat(deviceTypeRank(VK_PHYSICAL_DEVICE_TYPE_CPU))
            .isGreaterThan(deviceTypeRank(VK_PHYSICAL_DEVICE_TYPE_OTHER))
    }

    @Test
    fun `unknown device type values rank the same as OTHER`() {
        assertThat(deviceTypeRank(-1)).isEqualTo(deviceTypeRank(VK_PHYSICAL_DEVICE_TYPE_OTHER))
    }

    @Test
    fun `sums only device-local heaps`() {
        val heaps = listOf(
            HeapInfo(flags = VK_MEMORY_HEAP_DEVICE_LOCAL_BIT, size = 4_096L),
            HeapInfo(flags = 0, size = 8_192L),
        )

        assertThat(sumDeviceLocalMemory(heaps)).isEqualTo(4_096L)
    }

    @Test
    fun `sums multiple device-local heaps`() {
        val heaps = listOf(
            HeapInfo(flags = VK_MEMORY_HEAP_DEVICE_LOCAL_BIT, size = 1_024L),
            HeapInfo(flags = VK_MEMORY_HEAP_DEVICE_LOCAL_BIT, size = 2_048L),
        )

        assertThat(sumDeviceLocalMemory(heaps)).isEqualTo(3_072L)
    }

    @Test
    fun `returns zero when no heap is device-local`() {
        val heaps = listOf(HeapInfo(flags = 0, size = 4_096L))

        assertThat(sumDeviceLocalMemory(heaps)).isZero()
    }

    @Test
    fun `returns zero for an empty heap list`() {
        assertThat(sumDeviceLocalMemory(emptyList())).isZero()
    }

    @Test
    fun `dedicated queue rejects a family with both compute and graphics`() {
        val queueFlags = VK_QUEUE_COMPUTE_BIT or VK_QUEUE_GRAPHICS_BIT

        assertThat(isComputeRequirementSatisfied(queueFlags, dedicatedComputeQueue = true)).isFalse()
    }

    @Test
    fun `dedicated queue accepts a compute-only family`() {
        val queueFlags = VK_QUEUE_COMPUTE_BIT

        assertThat(isComputeRequirementSatisfied(queueFlags, dedicatedComputeQueue = true)).isTrue()
    }

    @Test
    fun `dedicated queue accepts a compute family with other non-graphics bits`() {
        val queueFlags = VK_QUEUE_COMPUTE_BIT or VK_QUEUE_TRANSFER_BIT

        assertThat(isComputeRequirementSatisfied(queueFlags, dedicatedComputeQueue = true)).isTrue()
    }

    @Test
    fun `dedicated queue rejects a graphics-only family`() {
        val queueFlags = VK_QUEUE_GRAPHICS_BIT

        assertThat(isComputeRequirementSatisfied(queueFlags, dedicatedComputeQueue = true)).isFalse()
    }

    @Test
    fun `non-dedicated queue accepts a family with both compute and graphics`() {
        val queueFlags = VK_QUEUE_COMPUTE_BIT or VK_QUEUE_GRAPHICS_BIT

        assertThat(isComputeRequirementSatisfied(queueFlags, dedicatedComputeQueue = false)).isTrue()
    }

    @Test
    fun `non-dedicated queue rejects a family without compute at all`() {
        val queueFlags = VK_QUEUE_GRAPHICS_BIT

        assertThat(isComputeRequirementSatisfied(queueFlags, dedicatedComputeQueue = false)).isFalse()
    }

    @Test
    fun `device name filter matches a substring`() {
        assertThat(isDeviceNameMatch("AMD Radeon 890M Graphics (RADV GFX1150)", "Radeon")).isTrue()
    }

    @Test
    fun `device name filter ignores case`() {
        assertThat(isDeviceNameMatch("AMD Radeon 890M Graphics (RADV GFX1150)", "radeon")).isTrue()
    }

    @Test
    fun `device name filter matches the full name`() {
        val name = "NVIDIA GeForce RTX 4090"

        assertThat(isDeviceNameMatch(name, name)).isTrue()
    }

    @Test
    fun `device name filter rejects an unrelated name`() {
        assertThat(isDeviceNameMatch("AMD Radeon 890M Graphics (RADV GFX1150)", "NVIDIA")).isFalse()
    }
}
