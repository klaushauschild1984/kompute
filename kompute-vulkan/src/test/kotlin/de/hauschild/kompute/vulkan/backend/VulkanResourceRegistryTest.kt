package de.hauschild.kompute.vulkan.backend

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class VulkanResourceRegistryTest {
    @Test
    fun `closing the registry closes all open resources`() {
        val registry = VulkanResourceRegistry()
        val first = Resource(registry).also { registry.register(it) }
        val second = Resource(registry).also { registry.register(it) }

        registry.close()

        assertThat(first.closeCount).isEqualTo(1)
        assertThat(second.closeCount).isEqualTo(1)
    }

    @Test
    fun `a resource that was closed before is not closed again`() {
        val registry = VulkanResourceRegistry()
        val resource = Resource(registry).also { registry.register(it) }
        resource.close()

        registry.close()

        assertThat(resource.closeCount).isEqualTo(1)
    }

    @Test
    fun `registering after the registry was closed is rejected`() {
        val registry = VulkanResourceRegistry().also { it.close() }

        assertThatThrownBy { registry.register(Resource(registry)) }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    @Suppress("ExplicitGarbageCollectionCall") // the Cleaner only runs after a garbage collection
    fun `the resource of an unreachable owner is released`() {
        val registry = VulkanResourceRegistry()
        val resource = Resource(registry).also { registry.register(it) }
        registerShortLivedOwner(registry, resource)

        var attempts = 0
        while (resource.closeCount == 0 && attempts++ < MAX_GC_ATTEMPTS) {
            System.gc()
            Thread.sleep(GC_WAIT_MILLIS)
            registry.flushUnreachable()
        }

        assertThat(resource.closeCount).isEqualTo(1)
    }

    private fun registerShortLivedOwner(
        registry: VulkanResourceRegistry,
        resource: AutoCloseable,
    ) = registry.closeWhenUnreachable(Any(), resource)

    private class Resource(private val registry: VulkanResourceRegistry) : AutoCloseable {
        var closeCount = 0
            private set

        override fun close() {
            closeCount++
            registry.unregister(this)
        }
    }

    companion object {
        private const val MAX_GC_ATTEMPTS = 100
        private const val GC_WAIT_MILLIS = 20L
    }
}
