package de.hauschild.kompute.vulkan.data

import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.exception.KomputeBackendInitializationException
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import de.hauschild.kompute.vulkan.VulkanTestContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VulkanStorageBufferTest {
    private val testContext = VulkanTestContext()
    private val context = testContext.context

    @AfterAll
    fun tearDown() {
        testContext.close()
    }

    @Test
    fun `read-write buffer round-trips int data`() {
        val data = IntArray(1_024) { it * 3 - 7 }
        val buffer = VulkanStorageBuffer(StorageBuffer<IntArray>(0).data(data).asOutput(), context)

        buffer.use { buffer ->
            buffer.bind()
            assertThat(buffer.read()).isEqualTo(data)
        }
    }

    @Test
    fun `read-write buffer round-trips float, long, double and byte data`() {
        VulkanStorageBuffer(StorageBuffer<FloatArray>(0).data(floatArrayOf(1.5f, -2f, 0.25f)).asOutput(), context)
            .use { buffer ->
                buffer.bind()
                assertThat(buffer.read()).containsExactly(1.5f, -2f, 0.25f)
            }
        VulkanStorageBuffer(StorageBuffer<LongArray>(0).data(longArrayOf(Long.MIN_VALUE, 42L)).asOutput(), context)
            .use { buffer ->
                buffer.bind()
                assertThat(buffer.read()).containsExactly(Long.MIN_VALUE, 42L)
            }
        VulkanStorageBuffer(StorageBuffer<DoubleArray>(0).data(doubleArrayOf(Math.PI, -0.5)).asOutput(), context)
            .use { buffer ->
                buffer.bind()
                assertThat(buffer.read()).containsExactly(Math.PI, -0.5)
            }
        VulkanStorageBuffer(StorageBuffer<ByteArray>(0).data(byteArrayOf(1, -1, 127)).asOutput(), context)
            .use { buffer ->
                buffer.bind()
                assertThat(buffer.read()).containsExactly(1, -1, 127)
            }
    }

    @Test
    fun `output buffer reads back the configured number of elements`() {
        VulkanStorageBuffer(StorageBuffer<IntArray>(0).size(16).asOutput(), context).use { buffer ->
            buffer.bind()
            assertThat(buffer.read()).hasSize(16)
        }
    }

    @Test
    fun `binding the same source again reuses the persistent buffer and its content`() {
        val source = StorageBuffer<IntArray>(0).data(intArrayOf(1, 2, 3)).asOutput()
        val first = VulkanStorageBuffer(source, context).also { it.bind() }
        val handle = first.handle()
        first.close()

        VulkanStorageBuffer(source, context).use { second ->
            second.bind()

            assertThat(second.handle()).isEqualTo(handle)
            assertThat(second.read()).containsExactly(1, 2, 3)
        }
    }

    @Test
    fun `a changed source size replaces the buffer`() {
        val source = StorageBuffer<IntArray>(0).data(intArrayOf(1, 2, 3)).asOutput()
        VulkanStorageBuffer(source, context).use { buffer ->
            buffer.bind()
            source.data(intArrayOf(4, 5, 6, 7))
            buffer.bind()

            assertThat(buffer.read()).containsExactly(4, 5, 6, 7)
        }
    }

    @Test
    fun `buffer larger than the device limit is rejected`() {
        testContext.contextWith(context.limits.copy(maxStorageBufferRange = 8)).use { limited ->
            val buffer = VulkanStorageBuffer(StorageBuffer<ByteArray>(0).size(16).asOutput(), limited)

            assertThatThrownBy { buffer.bind() }
                .isInstanceOf(KomputeConfigurationException::class.java)
                .hasMessageContaining("exceeds the device limit")
        }
    }

    @Test
    fun `reading before bind fails`() {
        val buffer = VulkanStorageBuffer(StorageBuffer<IntArray>(0).data(intArrayOf(1)).asOutput(), context)

        assertThatThrownBy { buffer.read() }.isInstanceOf(KomputeBackendInitializationException::class.java)
    }

    @Test
    fun `reading an input-only buffer fails`() {
        VulkanStorageBuffer(StorageBuffer<IntArray>(0).data(intArrayOf(1)), context).use { buffer ->
            buffer.bind()
            assertThatThrownBy { buffer.read() }.isInstanceOf(KomputeConfigurationException::class.java)
        }
    }

    @Test
    fun `empty buffer is rejected`() {
        val buffer = VulkanStorageBuffer(StorageBuffer<IntArray>(0).data(IntArray(0)).asOutput(), context)

        assertThatThrownBy { buffer.bind() }.isInstanceOf(KomputeConfigurationException::class.java)
    }

    @Test
    fun `close is idempotent`() {
        val buffer = VulkanStorageBuffer(StorageBuffer<IntArray>(0).data(intArrayOf(1)).asOutput(), context)
        buffer.bind()

        buffer.close()
        buffer.close()
    }
}
