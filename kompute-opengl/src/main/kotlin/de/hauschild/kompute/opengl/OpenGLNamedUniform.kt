package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.NamedUniform
import de.hauschild.kompute.core.exception.requireConfiguration
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL21
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL40

/**
 * @param T
 * @param program
 * @param source
 */
class OpenGLNamedUniform<T : Any>(
    private val program: OpenGLProgram,
    private val source: NamedUniform<T>
):Bindable {
    override fun bind() {
        val location = GL20.glGetUniformLocation(program.glHandle, source.name)
        requireConfiguration(location != -1) { "Named uniform '${source.name}' not found in shader" }
        when (source.type) {
            Int::class -> if (source.unsigned) {
                GL30.glUniform1ui(location, source.value as Int)
            } else {
                GL20.glUniform1i(location, source.value as Int)
            }
            Float::class -> GL20.glUniform1f(location, source.value as Float)
            Double::class -> GL40.glUniform1d(location, source.value as Double)
            Boolean::class -> GL20.glUniform1i(location, if (source.value as Boolean) 1 else 0)
            IntArray::class -> bindIntArray(location)
            FloatArray::class -> bindFloatArray(location)
            DoubleArray::class -> bindDoubleArray(location)

        }
    }

    private fun bindIntArray(location: Int) {
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

    private fun bindFloatArray(location: Int) {
        val v = source.value as FloatArray
        val dim = source.matrixDimension
        dim?.let {
            if (dim.rows == dim.columns) {
                when (dim.rows) {
                    2 -> GL20.glUniformMatrix2fv(location, false, v)
                    3 -> GL20.glUniformMatrix3fv(location, false, v)
                    4 -> GL20.glUniformMatrix4fv(location, false, v)
                }
            } else {
                when (dim.rows to dim.columns) {
                    2 to 3 -> GL21.glUniformMatrix3x2fv(location, false, v)
                    2 to 4 -> GL21.glUniformMatrix4x2fv(location, false, v)
                    3 to 2 -> GL21.glUniformMatrix2x3fv(location, false, v)
                    3 to 4 -> GL21.glUniformMatrix4x3fv(location, false, v)
                    4 to 2 -> GL21.glUniformMatrix2x4fv(location, false, v)
                    4 to 3 -> GL21.glUniformMatrix3x4fv(location, false, v)
                }
            }
        } ?: when (v.size) {
            2 -> GL20.glUniform2fv(location, v)
            3 -> GL20.glUniform3fv(location, v)
            4 -> GL20.glUniform4fv(location, v)
            else -> throw IllegalArgumentException("Invalid array size: ${v.size}")
        }
    }

    private fun bindDoubleArray(location: Int) {
        val v = source.value as DoubleArray
        val dim = source.matrixDimension
        dim?.let {
            if (dim.rows == dim.columns) {
                when (dim.rows) {
                    2 -> GL40.glUniformMatrix2dv(location, false, v)
                    3 -> GL40.glUniformMatrix3dv(location, false, v)
                    4 -> GL40.glUniformMatrix4dv(location, false, v)
                }
            } else {
                when (dim.rows to dim.columns) {
                    2 to 3 -> GL40.glUniformMatrix3x2dv(location, false, v)
                    2 to 4 -> GL40.glUniformMatrix4x2dv(location, false, v)
                    3 to 2 -> GL40.glUniformMatrix2x3dv(location, false, v)
                    3 to 4 -> GL40.glUniformMatrix4x3dv(location, false, v)
                    4 to 2 -> GL40.glUniformMatrix2x4dv(location, false, v)
                    4 to 3 -> GL40.glUniformMatrix3x4dv(location, false, v)
                }
            }
        } ?: when (v.size) {
            2 -> GL40.glUniform2dv(location, v)
            3 -> GL40.glUniform3dv(location, v)
            4 -> GL40.glUniform4dv(location, v)
            else -> throw IllegalArgumentException("Invalid array size: ${v.size}")
        }
    }

    override fun close() {
        // nothing to do
    }
}
