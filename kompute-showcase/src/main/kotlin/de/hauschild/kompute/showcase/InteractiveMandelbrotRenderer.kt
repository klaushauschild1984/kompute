package de.hauschild.kompute.showcase

import de.hauschild.kompute.showcase.MandelbrotRenderer.Config
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Point
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Interactive Swing window for exploring the Mandelbrot set in real time.
 *
 * - **Scroll** — zoom in and out
 * - **Drag** — pan the viewport
 * - **Page Up / Page Down** — double or halve the iteration limit
 * - **Home** — reset to the default view
 */
class InteractiveMandelbrotRenderer: JFrame("") {
    private val mandelbrotRenderer = MandelbrotRenderer()
    private var config: Config = Config()
    private var image: BufferedImage = mandelbrotRenderer.render(config)
    private val panel = object : JPanel() {
        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)
            graphics.drawImage(image, 0, 0, null)
        }
    }
    private var resizeTimer = Timer(200) {
        config = config.copy(width = contentPane.width, height = contentPane.height)
        panel.preferredSize = Dimension(config.width, config.height)
        render()
    }.apply { isRepeats = false }
    private var dragStart: Point? = null

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        add(panel)

        panel.preferredSize = Dimension(config.width, config.height)
        pack()

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) {
                resizeTimer.restart()
            }
        })
        panel.addMouseWheelListener { event ->
            val factor = if (event.wheelRotation < 0) 1.1 else 0.9
            config = config.copy(zoom = config.zoom * factor)
            render()
        }
        panel.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                dragStart = event.point
            }
            override fun mouseReleased(event: MouseEvent) {
                dragStart = null
            }
        })
        panel.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                val start = dragStart ?: return
                val dx = (e.x - start.x).toDouble() / config.width * 2.0 / config.zoom
                val dy = (e.y - start.y).toDouble() / config.height * 2.0 / config.zoom
                config = config.copy(centerX = config.centerX - dx, centerY = config.centerY - dy)
                dragStart = e.point
                render()
            }
        })
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                config = when (event.keyCode) {
                    KeyEvent.VK_PAGE_UP -> config.copy(maxIterations = config.maxIterations * 2)
                    KeyEvent.VK_PAGE_DOWN -> config.copy(maxIterations = maxOf(2, config.maxIterations / 2))
                    KeyEvent.VK_HOME -> Config()
                    else -> return
                }
                render()
            }
        })

        setLocationRelativeTo(null)
        isVisible = true
    }

    /**
     * Renders the current [config] and repaints the panel.
     */
    fun render() {
        titel()
        image = mandelbrotRenderer.render(config)
        panel.repaint()
    }

    /**
     * Updates the window title to reflect the current [config].
     */
    fun titel() {
        title = "Kompute - Mandelbrot set - $config"
    }

    override fun dispose() {
        super.dispose()
        mandelbrotRenderer.close()
    }
}

fun main() {
    SwingUtilities.invokeLater {
        InteractiveMandelbrotRenderer()
    }
}
