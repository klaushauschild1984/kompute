package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.Backend
import de.hauschild.kompute.core.Kompute
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class OpenGLBackendExtension :
    BeforeAllCallback,
    AfterAllCallback,
    ParameterResolver {
    override fun beforeAll(context: ExtensionContext?) {
        val backend = Kompute.openGL()
        context!!.getStore(NAMESPACE).put(BACKEND, backend)
    }

    override fun afterAll(context: ExtensionContext?) {
        context!!.getStore(NAMESPACE).get(BACKEND, Backend::class.java).close()
    }

    override fun supportsParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?,
    ): Boolean = parameterContext!!.parameter!!.type == Backend::class.java

    override fun resolveParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?,
    ): Any? = extensionContext!!.getStore(NAMESPACE)!!.get(BACKEND, Backend::class.java)

    companion object {
        const val BACKEND = "backend"
        val NAMESPACE: ExtensionContext.Namespace? = ExtensionContext.Namespace.create(OpenGLBackendExtension::class)
    }
}
