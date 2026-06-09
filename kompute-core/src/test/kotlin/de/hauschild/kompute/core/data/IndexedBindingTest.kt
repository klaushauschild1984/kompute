package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IndexedBindingTest {
    @Test
    fun `index validation`() {
        val exception = assertFailsWith<KomputeConfigurationException> {
            Buffer(-1).validate()
        }
        assertEquals("Index must be non-negative", exception.message)
    }

    @Test
    fun `cross validation`() {
        val exception =
            assertFailsWith<KomputeConfigurationException> {
                IndexedBinding.crossValidate(
                    listOf(
                        Buffer(0),
                        Buffer(0),
                    ),
                )
            }
        assertEquals("Duplicate index: [0]", exception.message)
    }

    private data class Buffer(override val index: Int) : IndexedBinding
}
