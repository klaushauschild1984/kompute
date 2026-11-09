package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.vulkan.VulkanBindable

/**
 * Hands out the descriptor set for a dispatch and reuses it as long as the dispatch binds the same
 * persistent buffers.
 *
 * A descriptor set points to concrete buffers, so it can only be reused if all resources of the
 * dispatch are persistent ([VulkanBindable.descriptorKey] is not `null`) and have not been replaced.
 * Dispatches with transient resources, such as a uniform buffer or an image, get a descriptor set of
 * their own that is released after the dispatch.
 *
 * At most [MAX_CACHED] sets are kept, the least recently used one is released first. All dispatches
 * through one cache are serialized, so a set is never released while a submission still uses it.
 *
 * @param device the [VulkanDevice]
 * @param layout the raw `VkDescriptorSetLayout` handle the sets are allocated with
 * @param bindings the reflected shader bindings
 * @param resources the registry that releases the sets with the backend
 */
internal class VulkanDescriptorSetCache(
    private val device: VulkanDevice,
    private val layout: Long,
    private val bindings: List<VulkanShaderBinding>,
    private val resources: VulkanResourceRegistry,
) : AutoCloseable {
    private val cached = LinkedHashMap<List<Any>, VulkanDescriptorSet>(INITIAL_CAPACITY, LOAD_FACTOR, true)

    /**
     * The number of descriptor sets created so far, which tests use to check the reuse.
     */
    var createdCount = 0L
        private set

    /**
     * Returns the number of descriptor sets currently kept for reuse.
     *
     * @return the number of cached descriptor sets
     */
    @Synchronized
    fun cachedCount(): Int = cached.size

    /**
     * Runs [block] with the descriptor set for [bindables] and returns its result.
     *
     * @param bindables the resources bound for the dispatch, already bound
     * @param block uses the descriptor set, `null` if the dispatch has no resources
     * @return the result of [block]
     * @throws VulkanTimeoutException if [block] does, the set stays alive as the GPU may still use it
     */
    @Synchronized
    fun <R> withDescriptorSet(
        bindables: List<VulkanBindable>,
        block: (VulkanDescriptorSet?) -> R,
    ): R {
        if (bindables.isEmpty()) {
            return block(null)
        }
        val key = bindables.map { it.descriptorKey() }
        return if (key.all { it != null }) {
            @Suppress("UNCHECKED_CAST")
            block(cachedSet(key as List<Any>, bindables))
        } else {
            transientSet(bindables, block)
        }
    }

    @Synchronized
    override fun close() {
        val sets = cached.values.toList()
        cached.clear()
        sets.forEach { release(it) }
    }

    private fun cachedSet(
        key: List<Any>,
        bindables: List<VulkanBindable>,
    ): VulkanDescriptorSet {
        cached[key]?.let { return it }
        releaseStale()
        val descriptorSet = create(bindables)
        cached[key] = descriptorSet
        resources.register(descriptorSet)
        while (cached.size > MAX_CACHED) {
            val eldest = cached.entries.iterator()
            val evicted = eldest.next().value
            eldest.remove()
            release(evicted)
        }
        return descriptorSet
    }

    private fun <R> transientSet(
        bindables: List<VulkanBindable>,
        block: (VulkanDescriptorSet?) -> R,
    ): R {
        val descriptorSet = create(bindables)
        val result = try {
            block(descriptorSet)
        } catch (timeout: VulkanTimeoutException) {
            // the GPU may still use the descriptor set, it is released with the backend
            resources.register(descriptorSet)
            throw timeout
        }
        descriptorSet.close()
        return result
    }

    private fun create(bindables: List<VulkanBindable>): VulkanDescriptorSet {
        val descriptorSet = VulkanDescriptorSet(device, layout, bindings)
        descriptorSet.update(bindables)
        createdCount++
        return descriptorSet
    }

    /**
     * Releases the sets that point to a buffer which was closed meanwhile, e.g. because its size
     * changed. Their keys can never match again.
     */
    private fun releaseStale() {
        val stale = cached.entries.filter { (key, _) -> key.any { it.isStale() } }
        stale.forEach { (key, descriptorSet) ->
            cached.remove(key)
            release(descriptorSet)
        }
    }

    private fun Any.isStale(): Boolean = this is Pair<*, *> && (second as? VulkanAllocatedBuffer)?.isClosed == true

    private fun release(descriptorSet: VulkanDescriptorSet) {
        resources.unregister(descriptorSet)
        descriptorSet.close()
    }

    companion object {
        private const val MAX_CACHED = 8
        private const val INITIAL_CAPACITY = 16
        private const val LOAD_FACTOR = 0.75f
    }
}
