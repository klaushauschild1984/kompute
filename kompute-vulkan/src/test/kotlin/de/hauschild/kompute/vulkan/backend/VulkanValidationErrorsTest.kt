package de.hauschild.kompute.vulkan.backend

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VulkanValidationErrorsTest {
    @Test
    fun `errors are recorded and can be read since an earlier count`() {
        val errors = VulkanValidationErrors()
        val before = errors.count()

        errors.record("first")
        errors.record("second")

        assertThat(errors.count()).isEqualTo(before + 2)
        assertThat(errors.since(before)).containsExactly("first", "second")
        assertThat(errors.since(before + 1)).containsExactly("second")
    }

    @Test
    fun `an empty log has no errors`() {
        assertThat(VulkanValidationErrors().since(0)).isEmpty()
    }
}
