package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.vulkan.VulkanBindable
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo
import org.lwjgl.vulkan.VkDescriptorPoolSize
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo
import org.lwjgl.vulkan.VkWriteDescriptorSet

/**
 * A descriptor set that lives for exactly one dispatch.
 *
 * Each instance owns a descriptor pool sized for the reflected shader bindings and a single set
 * allocated from it. Closing the instance destroys the pool, which frees the set.
 *
 * @param device the [VulkanDevice]
 * @param layout the raw `VkDescriptorSetLayout` handle the set is allocated with
 * @param bindings the reflected shader bindings, must not be empty
 */
internal class VulkanDescriptorSet(
    private val device: VulkanDevice,
    layout: Long,
    bindings: List<VulkanShaderBinding>,
) : AutoCloseable {
    private var pool: Long = VK10.VK_NULL_HANDLE

    /**
     * The raw `VkDescriptorSet` handle (non-dispatchable, hence a plain [Long]).
     */
    val handle: Long

    init {
        require(bindings.isNotEmpty()) { "A descriptor set needs at least one binding" }
        pool = createPool(bindings)
        handle = allocateSet(layout)
    }

    /**
     * Points the descriptors of this set to the resources of [bindables].
     *
     * @param bindables the bound resources, each one describes its own binding
     */
    fun update(bindables: List<VulkanBindable>) {
        MemoryStack.stackPush().use { stack ->
            val writes = VkWriteDescriptorSet.calloc(bindables.size, stack)
            bindables.forEachIndexed { index, bindable ->
                val write = writes[index]
                    .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(handle)
                    .dstArrayElement(0)
                bindable.describe(write, stack)
            }
            VK10.vkUpdateDescriptorSets(device.device, writes, null)
        }
    }

    override fun close() {
        if (pool != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyDescriptorPool(device.device, pool, null)
            pool = VK10.VK_NULL_HANDLE
        }
    }

    private fun createPool(bindings: List<VulkanShaderBinding>): Long = MemoryStack.stackPush().use { stack ->
        val countsByType = bindings.groupingBy { it.descriptorType }.eachCount().toList()
        val poolSizes = VkDescriptorPoolSize.calloc(countsByType.size, stack)
        countsByType.forEachIndexed { index, (type, count) ->
            poolSizes[index].type(type).descriptorCount(count)
        }
        val createInfo = VkDescriptorPoolCreateInfo.calloc(stack)
            .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
            .maxSets(1)
            .pPoolSizes(poolSizes)
        val poolPointer = stack.mallocLong(1)
        checkVk(VK10.vkCreateDescriptorPool(device.device, createInfo, null, poolPointer)) {
            "Failed to create Vulkan descriptor pool"
        }
        poolPointer.get(0)
    }

    private fun allocateSet(layout: Long): Long = MemoryStack.stackPush().use { stack ->
        val allocateInfo = VkDescriptorSetAllocateInfo.calloc(stack)
            .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
            .descriptorPool(pool)
            .pSetLayouts(stack.longs(layout))
        val setPointer = stack.mallocLong(1)
        val result = VK10.vkAllocateDescriptorSets(device.device, allocateInfo, setPointer)
        if (result != VK10.VK_SUCCESS) {
            close()
            checkVk(result) { "Failed to allocate Vulkan descriptor set" }
        }
        setPointer.get(0)
    }
}
