package de.hauschild.kompute.core.pipeline

import de.hauschild.kompute.core.data.OutputCapable
import de.hauschild.kompute.core.data.ShaderData
import de.hauschild.kompute.core.data.StorageBuffer
import de.hauschild.kompute.core.result.ResultReader
import de.hauschild.kompute.core.result.ShaderResult
import de.hauschild.kompute.core.shader.AbstractCompiledShader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class PipelineTest {
    @Test
    fun `stage shader is not closed after dispatch`() {
        val output = StorageBuffer<FloatArray>(0).size(1).asOutput()
        val closed = AtomicBoolean(false)
        val shader = object : AbstractCompiledShader() {
            override fun doDispatch(
                x: Int,
                y: Int,
                z: Int,
                data: List<ShaderData>
            ): ShaderResult =
                ShaderResult { emptyMap() }
            override fun close() {
                closed.set(true)
            }
        }

        Pipeline().execute(Stage(shader = shader, x = 1, data = listOf(output))).close()

        assertThat(closed.get()).isFalse()
    }

    @Test
    fun `pipeline result accesses last stage output`() {
        val output = StorageBuffer<FloatArray>(0).size(1).asOutput()
        val shader = object : AbstractCompiledShader() {
            override fun doDispatch(
                x: Int,
                y: Int,
                z: Int,
                data: List<ShaderData>
            ): ShaderResult =
                ShaderResult { mapOf(output to floatArrayOf(42f)) }
            override fun close() = Unit
        }

        Pipeline().execute(Stage(shader = shader, x = 1, data = listOf(output))).use { result ->
            assertThat(result[output][0]).isEqualTo(42f)
        }
    }

    @Test
    fun `pipeline close releases all accumulated results`() {
        val output = StorageBuffer<FloatArray>(0).size(1).asOutput()
        val closedCount = AtomicInteger(0)
        val shader = object : AbstractCompiledShader() {
            override fun doDispatch(
                x: Int,
                y: Int,
                z: Int,
                data: List<ShaderData>
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

        assertThat(closedCount.get()).isEqualTo(2)
    }
}
