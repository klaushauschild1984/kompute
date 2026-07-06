package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class AtomicCounterTest {
    @ParameterizedTest
    @MethodSource
    fun `validation fails`(atomicCounter: AtomicCounter){
        assertThatThrownBy { atomicCounter.validate() }.isInstanceOf(KomputeConfigurationException::class.java)
    }

    companion object {
        @JvmStatic
        fun `validation fails`() =
            listOf(
                AtomicCounter(-1),
                AtomicCounter(0).start(-1),
            )
    }
}
