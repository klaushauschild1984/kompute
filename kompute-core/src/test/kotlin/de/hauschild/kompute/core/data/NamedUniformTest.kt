package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

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
        assertThatThrownBy { namedUniform.validate() }.isInstanceOf(KomputeConfigurationException::class.java)
    }

    companion object {
        @JvmStatic
        fun `validation succeeds`() =
            listOf(
                NamedUniform<Float>("name").value(3.14f),
                NamedUniform<Int>("name").value(42),
                NamedUniform<Int>("name").value(42).asUnsigned(),
                NamedUniform<Double>("name").value(3.14),
                NamedUniform<Boolean>("name").value(true),
                NamedUniform<IntArray>("name").value(intArrayOf(1, 2, 3)),
                NamedUniform<IntArray>("name").value(intArrayOf(1, 2, 3)).asUnsigned(),
                NamedUniform<DoubleArray>("name").value(doubleArrayOf(1.0, 2.0)),
                NamedUniform<FloatArray>("name").value(floatArrayOf(1f, 2f, 3f, 4f)).asMatrix(2, 2),
            )

        @JvmStatic
        fun `validation fails`() =
            listOf(
                NamedUniform<Any>(""),
                NamedUniform<Any>("name"),
                NamedUniform<Float>("name").asUnsigned(),
                NamedUniform<FloatArray>("name").asUnsigned().asMatrix(4,4),
                NamedUniform<Int>("name").value(1).asUnsigned().asMatrix(2, 2),
                NamedUniform<Int>("name").value(1).asMatrix(2, 2),
                NamedUniform<FloatArray>("name").value(floatArrayOf(1f, 2f, 3f, 4f)).asMatrix(5, 2),
                NamedUniform<FloatArray>("name").value(floatArrayOf(1f, 2f, 3f)).asMatrix(2, 2),
                NamedUniform<FloatArray>("name").value(floatArrayOf(1f)),
                NamedUniform<IntArray>("name").value(intArrayOf(1, 2, 3, 4, 5)),
            )
    }
}
