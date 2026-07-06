package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class NamedBindingTest {
    @Test
    fun `index validation`() {
        assertThatThrownBy { Buffer("").validate() }
            .isInstanceOf(KomputeConfigurationException::class.java)
            .hasMessage("Name must not be blank")
    }

    @Test
    fun `cross validation`() {
        assertThatThrownBy {
            NamedBinding.crossValidate(
                listOf(
                    Buffer("name"),
                    Buffer("name"),
                ),
            )
        }
            .isInstanceOf(KomputeConfigurationException::class.java)
            .hasMessage("Duplicate name: [name]")
    }

    private data class Buffer(override val name: String) : NamedBinding
}
