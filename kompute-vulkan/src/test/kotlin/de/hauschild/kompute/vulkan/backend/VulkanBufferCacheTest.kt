package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.vulkan.VulkanTestContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.lwjgl.vulkan.VK10

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VulkanBufferCacheTest {
    private val testContext = VulkanTestContext()
    private val allocator = testContext.context.allocator

    @AfterAll
    fun tearDown() {
        testContext.close()
    }

    @Test
    fun `the same key gets the same buffer`() {
        VulkanBufferCache(allocator).use { cache ->
            val key = Any()

            val (first, firstIsNew) = cache.getOrAllocate(key, 16, USAGE)
            val (second, secondIsNew) = cache.getOrAllocate(key, 16, USAGE)

            assertThat(firstIsNew).isTrue()
            assertThat(secondIsNew).isFalse()
            assertThat(second).isSameAs(first)
        }
    }

    @Test
    fun `different keys get different buffers`() {
        VulkanBufferCache(allocator).use { cache ->
            val (first, _) = cache.getOrAllocate(Any(), 16, USAGE)
            val (second, secondIsNew) = cache.getOrAllocate(Any(), 16, USAGE)

            assertThat(second).isNotSameAs(first)
            assertThat(secondIsNew).isTrue()
        }
    }

    @Test
    fun `a different size replaces and releases the old buffer`() {
        VulkanBufferCache(allocator).use { cache ->
            val key = Any()
            val (old, _) = cache.getOrAllocate(key, 16, USAGE)

            val (replacement, isNew) = cache.getOrAllocate(key, 32, USAGE)

            assertThat(isNew).isTrue()
            assertThat(replacement.size).isEqualTo(32)
            assertThat(old.isClosed).isTrue()
        }
    }

    @Test
    fun `closing the cache releases all buffers and rejects further use`() {
        val cache = VulkanBufferCache(allocator)
        val key = Any()
        val (buffer, _) = cache.getOrAllocate(key, 16, USAGE)

        cache.close()

        assertThat(buffer.isClosed).isTrue()
        assertThatThrownBy { cache.getOrAllocate(key, 16, USAGE) }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `the buffer of an unreachable key is released`() {
        VulkanBufferCache(allocator).use { cache ->
            val buffer = allocateForShortLivedKey(cache)

            var attempts = 0
            while (!buffer.isClosed && attempts++ < MAX_GC_ATTEMPTS) {
                System.gc()
                Thread.sleep(GC_WAIT_MILLIS)
                cache.flushDeletions()
            }

            assertThat(buffer.isClosed).isTrue()
        }
    }

    private fun allocateForShortLivedKey(cache: VulkanBufferCache): VulkanAllocatedBuffer =
        cache.getOrAllocate(Any(), 16, USAGE).first

    companion object {
        private const val USAGE = VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
        private const val MAX_GC_ATTEMPTS = 100
        private const val GC_WAIT_MILLIS = 20L
    }
}
