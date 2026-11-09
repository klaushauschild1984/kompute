package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.Kompute
import de.hauschild.kompute.core.exception.KomputeBackendInitializationException
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class VulkanBackendTimeoutPropertyTest {
    @Test
    fun `a valid timeout is accepted`() {
        withTimeout("30") {
            assertThatCode { Kompute.vulkan().close() }.doesNotThrowAnyException()
        }
    }

    @Test
    fun `an invalid timeout fails the initialization with a clear message`() {
        withTimeout("soon") {
            assertThatThrownBy { Kompute.vulkan() }
                .isInstanceOf(KomputeBackendInitializationException::class.java)
                .hasMessageContaining("kompute.backend.vulkan.timeout must be a non-negative number of seconds")
        }
    }

    private fun withTimeout(
        value: String,
        block: () -> Unit,
    ) {
        System.setProperty(PROPERTY, value)
        try {
            block()
        } finally {
            System.clearProperty(PROPERTY)
        }
    }

    companion object {
        private const val PROPERTY = "kompute.backend.vulkan.timeout"
    }
}
