package de.hauschild.kompute.opengl

import de.hauschild.kompute.core.data.NamedUniform
import de.hauschild.kompute.core.exception.requireBackendInitialization
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL21
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL40

/**
 * Wraps an OpenGL shader name uniform for a [NamedUniform].
 *
 * @param T the data type — must be [Int], [Float], [Double], [Boolean], [IntArray], [FloatArray] or [DoubleArray]
 * @param program the linked OpenGL program to resolve the uniform location against
 * @param source the [NamedUniform] configuration this named uniform is based on
 */
class OpenGLNamedUniform<T : Any>(
    private val program: OpenGLProgram,
    private val source: NamedUniform<T>
):Bindable {
    override fun bind() {
        val location = GL20.glGetUniformLocation(program.glHandle, source.name)
        requireBackendInitialization(location != -1) { "Named uniform '${source.name}' not found in shader" }
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
            else -> error("Unsupported NamedUniform type: ${source.type.simpleName}")
        }
    }

    private fun bindIntArray(location: Int) {
        val value = source.value as IntArray
        if (source.unsigned) {
            when (value.size) {
                2 -> GL30.glUniform2uiv(location, value)
                3 -> GL30.glUniform3uiv(location, value)
                4 -> GL30.glUniform4uiv(location, value)
                else -> error("Invalid IntArray size: ${value.size}")
            }
        } else {
            when (value.size) {
                2 -> GL30.glUniform2iv(location, value)
                3 -> GL30.glUniform3iv(location, value)
                4 -> GL30.glUniform4iv(location, value)
                else -> error("Invalid IntArray size: ${value.size}")
            }
        }
    }

    private fun bindFloatArray(location: Int) {
        val v = source.value as FloatArray
        source.matrixDimension?.let { dim ->
            (FLOAT_MATRIX_BINDERS[dim.rows to dim.columns]
                ?: error("Invalid matrix dimension: ${dim.rows}×${dim.columns}"))
                .invoke(location, v)
        } ?: (FLOAT_VECTOR_BINDERS[v.size]
            ?: error("Invalid FloatArray size: ${v.size}"))
            .invoke(location, v)
    }

    private fun bindDoubleArray(location: Int) {
        val v = source.value as DoubleArray
        source.matrixDimension?.let { dim ->
            (DOUBLE_MATRIX_BINDERS[dim.rows to dim.columns]
                ?: error("Invalid matrix dimension: ${dim.rows}×${dim.columns}"))
                .invoke(location, v)
        } ?: (DOUBLE_VECTOR_BINDERS[v.size]
            ?: error("Invalid DoubleArray size: ${v.size}"))
            .invoke(location, v)
    }

    override fun close() {
        // nothing to do
    }

    companion object {
        private val FLOAT_MATRIX_BINDERS: Map<Pair<Int, Int>, (Int, FloatArray) -> Unit> = mapOf(
            (2 to 2) to { loc, v -> GL20.glUniformMatrix2fv(loc, false, v) },
            (3 to 3) to { loc, v -> GL20.glUniformMatrix3fv(loc, false, v) },
            (4 to 4) to { loc, v -> GL20.glUniformMatrix4fv(loc, false, v) },
            (2 to 3) to { loc, v -> GL21.glUniformMatrix2x3fv(loc, false, v) },
            (2 to 4) to { loc, v -> GL21.glUniformMatrix2x4fv(loc, false, v) },
            (3 to 2) to { loc, v -> GL21.glUniformMatrix3x2fv(loc, false, v) },
            (3 to 4) to { loc, v -> GL21.glUniformMatrix3x4fv(loc, false, v) },
            (4 to 2) to { loc, v -> GL21.glUniformMatrix4x2fv(loc, false, v) },
            (4 to 3) to { loc, v -> GL21.glUniformMatrix4x3fv(loc, false, v) },
        )
        private val FLOAT_VECTOR_BINDERS: Map<Int, (Int, FloatArray) -> Unit> = mapOf(
            2 to { loc, v -> GL20.glUniform2fv(loc, v) },
            3 to { loc, v -> GL20.glUniform3fv(loc, v) },
            4 to { loc, v -> GL20.glUniform4fv(loc, v) },
        )
        private val DOUBLE_MATRIX_BINDERS: Map<Pair<Int, Int>, (Int, DoubleArray) -> Unit> = mapOf(
            (2 to 2) to { loc, v -> GL40.glUniformMatrix2dv(loc, false, v) },
            (3 to 3) to { loc, v -> GL40.glUniformMatrix3dv(loc, false, v) },
            (4 to 4) to { loc, v -> GL40.glUniformMatrix4dv(loc, false, v) },
            (2 to 3) to { loc, v -> GL40.glUniformMatrix2x3dv(loc, false, v) },
            (2 to 4) to { loc, v -> GL40.glUniformMatrix2x4dv(loc, false, v) },
            (3 to 2) to { loc, v -> GL40.glUniformMatrix3x2dv(loc, false, v) },
            (3 to 4) to { loc, v -> GL40.glUniformMatrix3x4dv(loc, false, v) },
            (4 to 2) to { loc, v -> GL40.glUniformMatrix4x2dv(loc, false, v) },
            (4 to 3) to { loc, v -> GL40.glUniformMatrix4x3dv(loc, false, v) },
        )
        private val DOUBLE_VECTOR_BINDERS: Map<Int, (Int, DoubleArray) -> Unit> = mapOf(
            2 to { loc, v -> GL40.glUniform2dv(loc, v) },
            3 to { loc, v -> GL40.glUniform3dv(loc, v) },
            4 to { loc, v -> GL40.glUniform4dv(loc, v) },
        )
    }
}
