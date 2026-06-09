package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NamedBindingTest {
    @Test
    fun `index validation`() {
        val exception = assertFailsWith<KomputeConfigurationException> {
            Buffer("").validate()
        }
        assertEquals("Name must not be blank", exception.message)
    }

    @Test
    fun `cross validation`() {
        val exception =
            assertFailsWith<KomputeConfigurationException> {
                NamedBinding.crossValidate(
                    listOf(
                        Buffer("name"),
                        Buffer("name"),
                    ),
                )
            }
        assertEquals("Duplicate name: [name]", exception.message)
    }

    private data class Buffer(override val name: String) : NamedBinding
}
