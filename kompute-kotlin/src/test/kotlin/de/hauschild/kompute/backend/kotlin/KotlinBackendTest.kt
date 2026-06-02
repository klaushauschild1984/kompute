package de.hauschild.kompute.backend.kotlin

import de.hauschild.kompute.core.Kompute
import kotlin.test.Test
import kotlin.test.assertIs

class KotlinBackendTest {
    @Test
    fun `initialise Kotlin Backend`() {
        Kompute.kotlin().use { backend ->
            assertIs<KotlinBackend>(backend)
        }
    }
}
