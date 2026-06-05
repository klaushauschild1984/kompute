package de.hauschild.kompute.core

import de.hauschild.kompute.core.ShaderData.IndexBinding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IndexBindingTest {
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
                IndexBinding.crossValidate(
                    listOf(
                        Buffer(0),
                        Buffer(0),
                    ),
                )
            }
        assertEquals("There are duplicated indices: [0]", exception.message)
    }

    private data class Buffer(override val index: Int) : ShaderData.IndexBinding
}
