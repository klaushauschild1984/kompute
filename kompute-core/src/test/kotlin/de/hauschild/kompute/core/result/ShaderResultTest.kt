package de.hauschild.kompute.core.result

import de.hauschild.kompute.core.data.AtomicCounter
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ShaderResultTest {
    @Test
    fun `result data present`() {
        val atomicCounter = AtomicCounter(0)
        val shaderResult = ShaderResult { mutableMapOf(Pair(atomicCounter, 42)) }
        shaderResult.use {
            assertThat(it[atomicCounter]).isEqualTo(42)
        }
    }

    @Test
    fun `result data absent`() {
        val atomicCounter = AtomicCounter(0)
        val shaderResult = ShaderResult{ mutableMapOf() }
        shaderResult.use {
            assertThatThrownBy { it[atomicCounter] }.isInstanceOf(KomputeConfigurationException::class.java)
        }
    }
}
