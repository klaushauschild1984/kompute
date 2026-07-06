package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.Kompute
import de.hauschild.kompute.core.backend.Backend
import de.hauschild.kompute.opengl.backend.ContextCreationStrategy
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
 *
 * Available as a test fixture via `testImplementation(testFixtures(project(":kompute-opengl")))`.
 */
class OpenGLBackendExtension :
    BeforeAllCallback,
    ParameterResolver {
    override fun beforeAll(context: ExtensionContext) {
        context.root.getStore(NAMESPACE)
            .computeIfAbsent(BACKEND, { BackendResource(Kompute.openGL()) }, BackendResource::class.java)
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Boolean = Backend::class.java.isAssignableFrom(parameterContext.parameter.type)

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Any? = extensionContext.root
        .getStore(NAMESPACE)
        .get(BACKEND, BackendResource::class.java)
        ?.backend

    companion object {
        const val BACKEND = "backend"
        val NAMESPACE: ExtensionContext.Namespace = ExtensionContext.Namespace.create(OpenGLBackendExtension::class)

        /**
         * @property backend
         */
        private class BackendResource(val backend: Backend) : AutoCloseable {
            override fun close() {
                if (ContextCreationStrategy.isEglActive()) {
                    return
                }
                backend.close()
            }
        }
    }
}
