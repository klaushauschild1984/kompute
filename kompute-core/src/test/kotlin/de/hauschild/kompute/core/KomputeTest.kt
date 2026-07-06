package de.hauschild.kompute.core

import de.hauschild.kompute.core.backend.Backend
import de.hauschild.kompute.core.backend.TestBackend
import de.hauschild.kompute.core.exception.KomputeBackendInitializationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.Collections

class KomputeTest {
    @AfterEach
    fun resetTestBackend() {
        TestBackend.doInitializeAction = { /* no-op */ }
    }

    @Test
    fun `openGL discovers and initializes the registered backend`() {
        val backend = Kompute.openGL()

        assertThat(backend).isInstanceOf(TestBackend::class.java)
    }

    @Test
    fun `openGL propagates backend initialization failures`() {
        TestBackend.doInitializeAction = { throw KomputeBackendInitializationException("driver crash") }

        assertThatThrownBy { Kompute.openGL() }
            .isInstanceOf(KomputeBackendInitializationException::class.java)
            .hasMessage("driver crash")
    }

    @Test
    fun `openGL throws when no backend is registered`() {
        val emptyServicesClassLoader = object : ClassLoader(Kompute::class.java.classLoader) {
            override fun getResources(name: String) =
                if (name == "META-INF/services/${Backend::class.java.name}") {
                    Collections.emptyEnumeration()
                } else {
                    super.getResources(name)
                }
        }
        val originalClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = emptyServicesClassLoader
        try {
            assertThatThrownBy { Kompute.openGL() }
                .isInstanceOf(KomputeBackendInitializationException::class.java)
                .hasMessageContaining("No Backend found")
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }
}
