package de.hauschild.kompute.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SpotLightTest {
    @Test
    fun `roundtrips through byte array`() {
        val original = SpotLight(
            position = Vector3f(1f, 2f, 3f),
            direction = Vector3f(0f, -1f, 0f),
            color = Vector3f(1f, 1f, 1f),
            intensity = 1.5f,
            constantAttenuation = 1f,
            linearAttenuation = 0.09f,
            quadraticAttenuation = 0.032f,
            innerCutOff = 0.97f,
            outerCutOff = 0.9f,
        )

        val restored = original.toByteArray().fromByteArray(SpotLight::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `matches its std140 size`() {
        assertThat(SpotLight::class.sizeOf()).isEqualTo(80)
    }
}
