package de.hauschild.kompute.vulkan

import de.hauschild.kompute.vulkan.backend.VulkanValidationErrors
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Fails every test during which the Khronos validation layer reported an error.
 *
 * Registered for all tests of the module through JUnit's extension auto-detection. It has an effect
 * only when the validation layer is enabled (`-PvulkanValidation=true`, which sets the system
 * property `kompute.backend.vulkan.validation`), otherwise no messages arrive.
 */
class VulkanValidationExtension :
    BeforeEachCallback,
    AfterEachCallback {
    private val namespace = ExtensionContext.Namespace.create(VulkanValidationExtension::class)

    override fun beforeEach(context: ExtensionContext) {
        context.getStore(namespace).put(ERRORS_BEFORE, VulkanValidationErrors.global.count())
    }

    override fun afterEach(context: ExtensionContext) {
        val before = checkNotNull(context.getStore(namespace).get(ERRORS_BEFORE, Int::class.javaObjectType))
        assertThat(VulkanValidationErrors.global.since(before)).describedAs("Vulkan validation errors").isEmpty()
    }

    companion object {
        private const val ERRORS_BEFORE = "errorsBefore"
    }
}
