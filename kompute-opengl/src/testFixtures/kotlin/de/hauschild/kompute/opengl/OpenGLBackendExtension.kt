package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.Kompute
import de.hauschild.kompute.core.backend.Backend
import de.hauschild.kompute.opengl.backend.OpenGLBackend
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

/**
 * JUnit 5 extension that manages an [OpenGLBackend] lifecycle for integration tests.
 *
 * Creates a single [OpenGLBackend] instance shared across the entire test suite and closes it
 * when the root [ExtensionContext] store is closed at the end of the test run.
 * Injects the backend as a parameter into test methods that declare a [Backend] parameter.
 */
class OpenGLBackendExtension :
    BeforeAllCallback,
    ParameterResolver {
    override fun beforeAll(context: ExtensionContext?) {
        requireNotNull(context).root.getStore(NAMESPACE)
            .getOrComputeIfAbsent(BACKEND) { BackendResource(Kompute.openGL()) }
    }

    override fun supportsParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?,
    ): Boolean = Backend::class.java.isAssignableFrom(requireNotNull(parameterContext).parameter.type)

    override fun resolveParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?,
    ): Any? = requireNotNull(extensionContext).root
        .getStore(NAMESPACE)
        .get(BACKEND, BackendResource::class.java)
        ?.backend

    companion object {
        const val BACKEND = "backend"
        val NAMESPACE: ExtensionContext.Namespace = ExtensionContext.Namespace.create(OpenGLBackendExtension::class)

        private class BackendResource(val backend: Backend) : ExtensionContext.Store.CloseableResource {
            override fun close() = backend.close()
        }
    }
}
