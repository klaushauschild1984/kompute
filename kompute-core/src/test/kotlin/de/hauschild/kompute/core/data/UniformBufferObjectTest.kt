package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class UniformBufferObjectTest {
    @ParameterizedTest
    @MethodSource
    fun `validation fails`(uniformBufferObject: UniformBufferObject){
        assertFailsWith<KomputeConfigurationException> { uniformBufferObject.validate() }
    }

    companion object {
        @JvmStatic
        fun `validation fails`() =
            listOf(
                UniformBufferObject(-1),
                UniformBufferObject(0),
            )
    }
}
