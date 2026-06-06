package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.NamedUniform
import de.hauschild.kompute.core.exception.requireConfiguration
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL40

class OpenGLNamedUniform<T : Any>(
    private val program: OpenGLProgram,
    private val source: NamedUniform<T>
):Bindable {
    override fun bind() {
        val location = GL20.glGetUniformLocation(program.glHandle, source.name)
        requireConfiguration(location != -1) { "Named uniform '${source.name}' not found in shader" }
        when (source.type) {
            Int::class -> {
                if (source.unsigned) {
                    GL30.glUniform1ui(location, source.value as Int)
                } else {
                    GL20.glUniform1i(location, source.value as Int)
                }
            }
            Float::class -> GL20.glUniform1f(location, source.value as Float)
            Double::class -> GL40.glUniform1d(location, source.value as Double)
            Boolean::class -> GL20.glUniform1i(location, if (source.value as Boolean) 1 else 0)
            IntArray::class -> {
                val value = source.value as IntArray
                if (source.unsigned) {
                    when (value.size) {
                        2 -> GL30.glUniform2uiv(location, value)
                        3 -> GL30.glUniform3uiv(location, value)
                        4 -> GL30.glUniform4uiv(location, value)
                    }
                } else {
                    when (value.size) {
                        2 -> GL30.glUniform2iv(location, value)
                        3 -> GL30.glUniform3iv(location, value)
                        4 -> GL30.glUniform4iv(location, value)
                    }
                }
            }
            FloatArray::class -> Unit
            DoubleArray::class -> Unit
        }
    }

    override fun close() {
        // nothing to do
    }
}
