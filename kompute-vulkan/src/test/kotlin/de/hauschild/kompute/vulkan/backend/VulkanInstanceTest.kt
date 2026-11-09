package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.KomputeBackendInitializationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VulkanInstanceTest {
    @Test
    fun `requesting validation either works or fails with a clear message`() {
        val instance = runCatching { VulkanInstance(validation = true) }

        instance.onSuccess { it.close() }
        instance.onFailure { failure ->
            assertThat(failure).isInstanceOf(KomputeBackendInitializationException::class.java)
            assertThat(failure).hasMessageContaining("VK_LAYER_KHRONOS_validation is not installed")
        }
    }
}
