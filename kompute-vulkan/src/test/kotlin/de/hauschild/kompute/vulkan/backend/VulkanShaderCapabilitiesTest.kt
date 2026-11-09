package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.lwjgl.util.spvc.Spv

class VulkanShaderCapabilitiesTest {
    @Test
    fun `shader without special capabilities is always supported`() {
        assertThatCode { VulkanShaderCapabilities.requireSupported(emptySet(), false, false) }
            .doesNotThrowAnyException()
    }

    @Test
    fun `supported 64 bit types are accepted`() {
        val declared = setOf(Spv.SpvCapabilityFloat64, Spv.SpvCapabilityInt64)

        assertThatCode { VulkanShaderCapabilities.requireSupported(declared, true, true) }.doesNotThrowAnyException()
    }

    @Test
    fun `unsupported 64 bit floats are rejected with a clear message`() {
        val declared = setOf(Spv.SpvCapabilityFloat64)

        assertThatThrownBy { VulkanShaderCapabilities.requireSupported(declared, false, true) }
            .isInstanceOf(KomputeConfigurationException::class.java)
            .hasMessageContaining("shaderFloat64")
    }

    @Test
    fun `unsupported 64 bit integers are rejected with a clear message`() {
        val declared = setOf(Spv.SpvCapabilityInt64)

        assertThatThrownBy { VulkanShaderCapabilities.requireSupported(declared, true, false) }
            .isInstanceOf(KomputeConfigurationException::class.java)
            .hasMessageContaining("shaderInt64")
    }
}
