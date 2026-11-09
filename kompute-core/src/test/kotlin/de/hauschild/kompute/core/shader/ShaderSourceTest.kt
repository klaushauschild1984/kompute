package de.hauschild.kompute.core.shader

import de.hauschild.kompute.core.exception.KomputeShaderSourceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class ShaderSourceTest {
    @Test
    fun `Glsl Code resolve returns the inline source unchanged`() {
        val source = ShaderSource.Glsl.Code("#version 430\nvoid main() {}")

        assertThat(source.resolve()).isEqualTo("#version 430\nvoid main() {}")
    }

    @Test
    fun `Glsl Stream resolve reads the complete stream as text`() {
        val source = ShaderSource.Glsl.Stream(ByteArrayInputStream("#version 430\nvoid main() {}".toByteArray()))

        assertThat(source.resolve()).isEqualTo("#version 430\nvoid main() {}")
    }

    @Test
    fun `Glsl Stream resolve closes the stream`() {
        val stream = TrackingInputStream(ByteArrayInputStream("#version 430".toByteArray()))
        val source = ShaderSource.Glsl.Stream(stream)

        source.resolve()

        assertThat(stream.closed).isTrue()
    }

    @Test
    fun `Glsl Stream resolve wraps IOException`() {
        val source = ShaderSource.Glsl.Stream(ThrowingInputStream())

        assertThatThrownBy { source.resolve() }
            .isInstanceOf(KomputeShaderSourceException::class.java)
            .hasCauseInstanceOf(IOException::class.java)
    }

    @Test
    fun `Glsl File resolve reads the file content`(
        @TempDir tempDir: Path,
    ) {
        val file = tempDir.resolve("shader.glsl")
        Files.writeString(file, "#version 430\nvoid main() {}")
        val source = ShaderSource.Glsl.File(file)

        assertThat(source.resolve()).isEqualTo("#version 430\nvoid main() {}")
    }

    @Test
    fun `Glsl File resolve wraps IOException for a missing file`(
        @TempDir tempDir: Path,
    ) {
        val source = ShaderSource.Glsl.File(tempDir.resolve("missing.glsl"))

        assertThatThrownBy { source.resolve() }
            .isInstanceOf(KomputeShaderSourceException::class.java)
            .hasCauseInstanceOf(IOException::class.java)
    }

    @Test
    fun `Spirv Bytecode resolve returns the inline bytecode unchanged`() {
        val bytecode = byteArrayOf(0x03, 0x02, 0x23, 0x07)
        val source = ShaderSource.Spirv.Bytecode(bytecode)

        assertThat(source.resolve()).isEqualTo(bytecode)
    }

    @Test
    fun `Spirv Stream resolve reads the complete stream as bytes`() {
        val bytecode = byteArrayOf(0x03, 0x02, 0x23, 0x07)
        val source = ShaderSource.Spirv.Stream(ByteArrayInputStream(bytecode))

        assertThat(source.resolve()).isEqualTo(bytecode)
    }

    @Test
    fun `Spirv Stream resolve closes the stream`() {
        val stream = TrackingInputStream(ByteArrayInputStream(byteArrayOf(0x03, 0x02, 0x23, 0x07)))
        val source = ShaderSource.Spirv.Stream(stream)

        source.resolve()

        assertThat(stream.closed).isTrue()
    }

    @Test
    fun `Spirv Stream resolve wraps IOException`() {
        val source = ShaderSource.Spirv.Stream(ThrowingInputStream())

        assertThatThrownBy { source.resolve() }
            .isInstanceOf(KomputeShaderSourceException::class.java)
            .hasCauseInstanceOf(IOException::class.java)
    }

    @Test
    fun `Spirv File resolve reads the file content`(
        @TempDir tempDir: Path,
    ) {
        val bytecode = byteArrayOf(0x03, 0x02, 0x23, 0x07)
        val file = tempDir.resolve("shader.spv")
        Files.write(file, bytecode)
        val source = ShaderSource.Spirv.File(file)

        assertThat(source.resolve()).isEqualTo(bytecode)
    }

    @Test
    fun `Spirv File resolve wraps IOException for a missing file`(
        @TempDir tempDir: Path,
    ) {
        val source = ShaderSource.Spirv.File(tempDir.resolve("missing.spv"))

        assertThatThrownBy { source.resolve() }
            .isInstanceOf(KomputeShaderSourceException::class.java)
            .hasCauseInstanceOf(IOException::class.java)
    }

    private class TrackingInputStream(
        private val delegate: InputStream,
    ) : InputStream() {
        var closed: Boolean = false
            private set

        override fun read(): Int = delegate.read()

        override fun close() {
            closed = true
            delegate.close()
        }
    }

    private class ThrowingInputStream : InputStream() {
        override fun read(): Int = throw IOException("boom")
    }
}
