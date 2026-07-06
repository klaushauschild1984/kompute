package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Image2DTest {
    @ParameterizedTest
    @MethodSource
    fun `validation fails`(image2D: Image2D) {
        assertThatThrownBy { image2D.validate() }.isInstanceOf(KomputeConfigurationException::class.java)
    }

    companion object {
        @JvmStatic
        fun `validation fails`() =
            listOf(
                Image2D(-1),
                Image2D(0),
                Image2D(0).dimension(0, 0),
                Image2D(0).dimension(5, 0),
                Image2D(0).dimension(0, 5),
            )
    }
}
