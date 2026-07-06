package de.hauschild.kompute.core.backend

import de.hauschild.kompute.core.InternalApi
import de.hauschild.kompute.core.exception.KomputeBackendInitializationException
import de.hauschild.kompute.core.exception.KomputeConfigurationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(InternalApi::class)
class AbstractBackendTest {
    @AfterEach
    fun resetTestBackend() {
        TestBackend.doInitializeAction = { /* no-op */ }
    }

    @Test
    fun `initialize can only be called once`() {
        val backend = TestBackend()
        backend.initialize()

        assertThatThrownBy { backend.initialize() }
            .isInstanceOf(KomputeBackendInitializationException::class.java)
            .hasMessage("Backend already initialized")
    }

    @Test
    fun `initialize wraps unexpected exceptions`() {
        TestBackend.doInitializeAction = { throw IllegalStateException("driver crash") }
        val backend = TestBackend()

        assertThatThrownBy { backend.initialize() }
            .isInstanceOf(KomputeBackendInitializationException::class.java)
            .hasMessage("Failed to initialize backend")
            .hasCauseInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `initialize passes through KomputeException unchanged`() {
        TestBackend.doInitializeAction = { throw KomputeConfigurationException("bad config") }
        val backend = TestBackend()

        assertThatThrownBy { backend.initialize() }
            .isInstanceOf(KomputeConfigurationException::class.java)
            .hasMessage("bad config")
    }
}
