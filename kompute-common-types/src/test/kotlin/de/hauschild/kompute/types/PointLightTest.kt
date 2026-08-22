package de.hauschild.kompute.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PointLightTest {
    @Test
    fun `roundtrips through byte array`() {
        val original = PointLight(
            position = Vector3f(1f, 2f, 3f),
            color = Vector3f(0.8f, 0.8f, 0.6f),
            intensity = 1.5f,
            constantAttenuation = 1f,
            linearAttenuation = 0.09f,
            quadraticAttenuation = 0.032f,
        )

        val restored = original.toByteArray().fromByteArray(PointLight::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `matches its std140 size`() {
        assertThat(PointLight::class.sizeOf()).isEqualTo(48)
    }
}
