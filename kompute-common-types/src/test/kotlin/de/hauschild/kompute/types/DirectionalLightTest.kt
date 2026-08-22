package de.hauschild.kompute.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DirectionalLightTest {
    @Test
    fun `roundtrips through byte array`() {
        val original = DirectionalLight(
            direction = Vector3f(1f, 2f, 3f),
            color = Vector3f(0.9f, 0.9f, 0.8f),
            intensity = 1.2f,
        )

        val restored = original.toByteArray().fromByteArray(DirectionalLight::class)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `matches its std140 size`() {
        assertThat(DirectionalLight::class.sizeOf()).isEqualTo(32)
    }
}
