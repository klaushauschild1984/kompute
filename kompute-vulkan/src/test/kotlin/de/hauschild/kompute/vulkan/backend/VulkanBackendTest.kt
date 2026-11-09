package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.vulkan.VulkanBackendExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VulkanBackendExtension::class)
class VulkanBackendTest {
    @Test
    fun `backend initialization`(backend: VulkanBackend) {
        assertThat(backend).isInstanceOf(VulkanBackend::class.java)
    }
}
