package de.hauschild.kompute.showcase

import de.hauschild.kompute.core.Kompute
import de.hauschild.kompute.core.backend.Backend

/**
 * @param closeBackend whether to close the backend after use - defaults to true
 * @property backend the [Backend] to be used for rendering - defaults to [Kompute.openGL]
 */
abstract class BackendUser(
    protected val backend: Backend = Kompute.openGL(),
    private val closeBackend: Boolean = true,
) : AutoCloseable {
    override fun close() {
        if (closeBackend) {
            backend.close()
        }
    }
}
