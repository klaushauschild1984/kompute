package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UniformBufferObjectTest {
    @ParameterizedTest
    @MethodSource
    fun `validation fails`(uniformBufferObject: UniformBufferObject){
        assertThatThrownBy { uniformBufferObject.validate() }.isInstanceOf(KomputeConfigurationException::class.java)
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
