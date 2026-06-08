package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class NamedUniformTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource
    fun `validation succeeds`(
        namedUniform: NamedUniform<*>,
    ) {
        namedUniform.validate()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    fun `validation fails`(
        namedUniform: NamedUniform<*>,
    ) {
        assertFailsWith<KomputeConfigurationException> { namedUniform.validate() }
    }

    companion object {
        @JvmStatic
        fun `validation succeeds`() =
            listOf(
                NamedUniform<Float>("name").value(3.14f),
                NamedUniform<Int>("name").value(42),
                NamedUniform<Int>("name").value(42).asUnsigned(),
            )

        @JvmStatic
        fun `validation fails`() =
            listOf(
                NamedUniform<Any>(""),
                NamedUniform<Any>("name"),
                NamedUniform<Float>("name").asUnsigned(),
                NamedUniform<FloatArray>("name").asUnsigned().asMatrix(4,4),
            )
    }
}
