package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class IndexedBindingTest {
    @Test
    fun `index validation`() {
        assertThatThrownBy { Buffer(-1).validate() }
            .isInstanceOf(KomputeConfigurationException::class.java)
            .hasMessage("Index must be non-negative")
    }

    @Test
    fun `cross validation`() {
        assertThatThrownBy {
            IndexedBinding.crossValidate(
                listOf(
                    Buffer(0),
                    Buffer(0),
                ),
            )
        }
            .isInstanceOf(KomputeConfigurationException::class.java)
            .hasMessage("Duplicate index: [0]")
    }

    private data class Buffer(override val index: Int) : IndexedBinding
}
