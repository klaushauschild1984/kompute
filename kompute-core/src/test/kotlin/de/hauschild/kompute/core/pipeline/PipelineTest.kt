package de.hauschild.kompute.core.pipeline

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.result.ResultReader
import de.hauschild.kompute.core.result.ShaderResult
import de.hauschild.kompute.core.shader.AbstractCompiledShader
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PipelineTest {
    @Test
    fun `stage shader is not closed after dispatch`() {
        val output = StorageBuffer<FloatArray>(0).size(1).asOutput()
        val closed = AtomicBoolean(false)
        val shader = object : AbstractCompiledShader() {
            override fun dispatch(
                x: Int,
                y: Int,
                z: Int,
                vararg data: ShaderData
            ): ShaderResult =
                ShaderResult { emptyMap() }
            override fun close() {
                closed.set(true)
            }
        }

        Pipeline().execute(Stage(shader = shader, x = 1, data = listOf(output))).close()

        assertTrue(!closed.get())
    }

    @Test
    fun `pipeline result accesses last stage output`() {
        val output = StorageBuffer<FloatArray>(0).size(1).asOutput()
        val shader = object : AbstractCompiledShader() {
            override fun dispatch(
                x: Int,
                y: Int,
                z: Int,
                vararg data: ShaderData
            ): ShaderResult =
                ShaderResult { mapOf(output to floatArrayOf(42f)) }
            override fun close() = Unit
        }

        Pipeline().execute(Stage(shader = shader, x = 1, data = listOf(output))).use { result ->
            assertEquals(42f, result[output][0])
        }
    }

    @Test
    fun `pipeline close releases all accumulated results`() {
        val output = StorageBuffer<FloatArray>(0).size(1).asOutput()
        val closedCount = AtomicInteger(0)
        val shader = object : AbstractCompiledShader() {
            override fun dispatch(
                x: Int,
                y: Int,
                z: Int,
                vararg data: ShaderData
            ): ShaderResult =
                ShaderResult(object : ResultReader {
                    override fun read(): Map<OutputCapable<*>, Any> = emptyMap()
                    override fun close() {
                        closedCount.incrementAndGet()
                    }
                })
            override fun close() = Unit
        }

        val stage1 = Stage(shader = shader, x = 1, data = listOf(output))
        val stage2 = Stage(shader = shader, x = 1, data = listOf(output))
        Pipeline().execute(stage1, stage2).close()

        assertEquals(2, closedCount.get())
    }
}
