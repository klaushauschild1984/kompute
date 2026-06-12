package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.result.ResultReader

/**
 * OpenGL implementation of [ResultReader].
 *
 * @param bindables list of [Bindable], the data is read from, if their source is marked as [OutputCapable.isOutput]
 */
internal class OpenGLResultReader(
    private val bindables: List<Bindable>
) : ResultReader {
    override fun read(): Map<OutputCapable<*>, Any> {
        val results = mutableMapOf<OutputCapable<*>, Any>()
        bindables.filterIsInstance<Readable<*>>()
            .filter { buffer -> buffer.source.isOutput }
            .forEach { buffer -> results[buffer.source] = buffer.read() }
        return results
    }

    override fun close() {
        bindables.forEach { it.close() }
    }
}
