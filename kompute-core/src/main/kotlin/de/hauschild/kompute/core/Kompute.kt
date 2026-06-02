package de.hauschild.kompute.core

import java.util.ServiceLoader

object Kompute {
    @JvmStatic
    fun kotlin(): Backend = load(Type.Kotlin)

    @JvmStatic
    fun openGL(): Backend = load(Type.OpenGL)

    @OptIn(InternalApi::class)
    private fun load(type: Type): Backend {
        val backend = (
            ServiceLoader
                .load(Backend::class.java)
                .firstOrNull { it.type() == type }
                ?: error("No Backend found for $type")
        )
        backend.initialize()
        return backend
    }
}
