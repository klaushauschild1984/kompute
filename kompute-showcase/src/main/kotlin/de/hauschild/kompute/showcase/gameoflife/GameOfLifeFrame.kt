package de.hauschild.kompute.showcase.gameoflife

import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Swing frame that runs the Game of Life simulation in a continuous loop.
 */
class GameOfLifeFrame : JFrame() {
    private val gameOfLife = GameOfLife()
    private var image = gameOfLife.step()
    private val panel = object : JPanel() {
        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)
            graphics.drawImage(image, 0, 0, width, height, null)
        }
    }
    private var lastFrameTime = System.nanoTime()
    private val loopTimer = Timer(1) {
        val now = System.nanoTime()
        val dt = (now - lastFrameTime) / 1_000_000_000.0f
        lastFrameTime = now
        image = gameOfLife.step()
        panel.repaint()
        title = "Kompute – Game of Life" +
                " | ${(1.0f / dt).toInt()} FPS" +
                " | Generation ${gameOfLife.generation}"
    }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        add(panel)
        panel.preferredSize = Dimension(
            GameOfLife.WIDTH * ZOOM,
            GameOfLife.HEIGHT * ZOOM,
        )
        pack()
        setLocationRelativeTo(null)
        isResizable = false
        isVisible = true
        loopTimer.start()
    }

    override fun dispose() {
        super.dispose()
        gameOfLife.close()
    }

    companion object {
        private const val ZOOM = 3
    }
}

fun main() {
    SwingUtilities.invokeLater { GameOfLifeFrame() }
}
