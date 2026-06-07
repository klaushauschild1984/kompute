package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.Kompute
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

/**
 * JUnit 5 extension that manages an [OpenGLBackend] lifecycle for integration tests.
 *
 * Creates a shared [Backend] instance before all tests and closes it after.
 * Injects the backend as a parameter into test methods that declare a [Backend] parameter.
 */
class OpenGLBackendExtension :
    BeforeAllCallback,
    AfterAllCallback,
    ParameterResolver {
    override fun beforeAll(context: ExtensionContext?) {
        val backend = Kompute.openGL()
        requireNotNull(context).getStore(NAMESPACE).put(BACKEND, backend)
    }

    override fun afterAll(context: ExtensionContext?) {
        val backend = requireNotNull(context).getStore(NAMESPACE).get(BACKEND, OpenGLBackend::class.java)
        backend?.close()
    }

    override fun supportsParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?,
    ): Boolean = requireNotNull(parameterContext).parameter.type == OpenGLBackend::class.java

    override fun resolveParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?,
    ): Any? = requireNotNull(extensionContext).getStore(NAMESPACE).get(BACKEND, OpenGLBackend::class.java)

    companion object {
        const val BACKEND = "backend"
        val NAMESPACE: ExtensionContext.Namespace = ExtensionContext.Namespace.create(OpenGLBackendExtension::class)
    }
}
