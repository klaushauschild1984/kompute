package de.hauschild.kompute.core.shader

import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShaderResultTest {
    @Test
    fun `result data present`() {
        val atomicCounter = AtomicCounter(0)
        val shaderResult = ShaderResult(mutableMapOf(Pair(atomicCounter, 42)))
        assertEquals(42 ,shaderResult[atomicCounter])
    }

    @Test
    fun `result data absent`() {
        val atomicCounter = AtomicCounter(0)
        val shaderResult = ShaderResult(mutableMapOf())
        assertFailsWith<KomputeConfigurationException> { shaderResult[atomicCounter] }
    }
}
