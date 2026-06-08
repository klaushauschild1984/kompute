package de.hauschild.kompute.core.data

import de.hauschild.kompute.core.data.Image2D.Format
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Image2DResultTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource
    fun toBufferedImage(
        bytes: ByteArray,
        format: Format,
        expectedColor: Long
    ) {
        val result = Image2D.Image2DResult(bytes, 1, 1, format)
        val image = result.toBufferedImage()!!

        val isRGB = image.colorModel.colorSpace.isCS_sRGB
        if (isRGB) {
            assertEquals(
                "0x${expectedColor.toString(16).uppercase()}",
                "0x${(image.getRGB(0, 0).toLong() and 0xFFFFFFFFL)
                    .toString(16).padStart(8, '0').uppercase()}"
            )
        } else {
            assertEquals(expectedColor.toUByte(), image.raster.getSample(0, 0, 0).toUByte())
        }
    }
    @Test
    fun `toBufferedImage not supported`() {
        Format::class.sealedSubclasses
            .mapNotNull { it.objectInstance }
            .filter { it.bufferedImageType == null }
            .forEach { format ->
                val result = Image2D.Image2DResult(ByteArray(0), 1, 1, format)
                assertNull(result.toBufferedImage())
            }
    }

    companion object {
        @JvmStatic
        fun toBufferedImage() = listOf<Arguments>(
            Arguments.of(Named.of("RGBA8 black", byteArrayOf(0, 0, 0, 255.toByte())), Format.RGBA8, 0xFF_00_00_00),
            Arguments.of(Named.of("RGBA8 red", byteArrayOf(255.toByte(), 0, 0, 255.toByte())), Format.RGBA8,
                0xFF_FF_00_00),
            Arguments.of(Named.of("RGBA8 green",byteArrayOf(0, 255.toByte(), 0, 255.toByte())), Format.RGBA8,
                0xFF_00_FF_00),
            Arguments.of(Named.of("RGBA8 blue",byteArrayOf(0, 0, 255.toByte(), 255.toByte())), Format.RGBA8,
                0xFF_00_00_FF),
            Arguments.of(Named.of("RGBA8 white",byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())),
                Format.RGBA8,
                0xFF_FF_FF_FF),
            Arguments.of(Named.of("R8 mid-gray",byteArrayOf(128.toByte())), Format.R8, 0xFF_80_80_80)
        )
    }
}
