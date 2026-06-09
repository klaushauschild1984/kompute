package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class AtomicCounterTest {
    @ParameterizedTest
    @MethodSource
    fun `validation fails`(atomicCounter: AtomicCounter){
        assertFailsWith<KomputeConfigurationException> { atomicCounter.validate() }
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
