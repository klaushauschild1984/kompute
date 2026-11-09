package de.hauschild.kompute.vulkan.backend

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import de.hauschild.kompute.core.exception.requireConfiguration
import org.lwjgl.util.spvc.Spv

/**
 * Checks the SPIR-V capabilities a shader declares against what the Vulkan device supports, so that
 * an unsupported shader fails with a clear message instead of an opaque pipeline creation error.
 */
internal object VulkanShaderCapabilities {
    /**
     * Throws if the shader declares a capability the device does not support.
     *
     * @param declared the SPIR-V capabilities the shader declares
     * @param float64Enabled whether the device supports 64 bit floating point types
     * @param int64Enabled whether the device supports 64 bit integer types
     * @throws KomputeConfigurationException if a declared capability is not supported
     */
    fun requireSupported(
        declared: Set<Int>,
        float64Enabled: Boolean,
        int64Enabled: Boolean,
    ) {
        requireConfiguration(float64Enabled || Spv.SpvCapabilityFloat64 !in declared) {
            "The shader uses `double`, but the Vulkan device lacks shaderFloat64 (Metal on macOS has no `double`)"
        }
        requireConfiguration(int64Enabled || Spv.SpvCapabilityInt64 !in declared) {
            "The shader uses `int64_t`, but the Vulkan device lacks the shaderInt64 feature"
        }
    }
}
