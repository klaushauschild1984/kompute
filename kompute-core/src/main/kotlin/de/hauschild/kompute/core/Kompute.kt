package de.hauschild.kompute.core

import de.hauschild.kompute.core.backend.Backend
import de.hauschild.kompute.core.backend.Type
import de.hauschild.kompute.core.exception.KomputeBackendInitializationException
import java.util.ServiceLoader

/**
 * Main entry point for accessing GPU compute backends.
 *
 * Discovers and initializes GPU compute backends using Java's [ServiceLoader] mechanism.
 * Backends must implement [de.hauschild.kompute.core.backend.Backend] and be registered in `META-INF/services`.
 *
 * Example usage:
 * ```
 * Kompute.openGL().use { openGL ->
 *     val output = StorageBuffer<FloatArray>(1).size(128).asOutput()
 *     val result = openGL
 *         .shader(ShaderSource.Code(glslCode))
 *         .data(
 *             StorageBuffer<FloatArray>(0).data(input),
 *             output,
 *         )
 *         .dispatch(x = 64)
 *         .execute()
 *     println(result[output].contentToString())
 * }
 * ```
 */
object Kompute {
    /**
     * Creates and initializes an OpenGL compute backend.
     *
     * @return an initialized [de.hauschild.kompute.core.backend.Backend] for OpenGL compute operations
     * @throws KomputeBackendInitializationException if no OpenGL backend is found or initialization fails
     */
    @JvmStatic
    fun openGL(): Backend = load(Type.OpenGL)

    @OptIn(InternalApi::class)
    private fun load(type: Type): Backend {
        val backend = (
                ServiceLoader
                    .load(Backend::class.java)
                    .firstOrNull { it.type() == type }
                    ?: throw KomputeBackendInitializationException("No Backend found for $type")
        )
        backend.initialize()
        return backend
    }
}
