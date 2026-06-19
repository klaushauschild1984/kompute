package de.hauschild.kompute.showcase.particlesimulation

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer

class ParticleSimulationFrame : JFrame("") {
    private val particleSimulation = ParticleSimulation()
    private var particleSimulationImage = particleSimulation.step(0f)
    private var mouse: Point = Point()
    private var mousePressed: Boolean = false
    private val panel = object : JPanel(){
        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)
            graphics.drawImage(
                particleSimulationImage,
                0,
                0,
                width,
                height,
                null
            )
        }
    }
    private var lastFrameTime = System.nanoTime()
    private val loopTimer = Timer(10) {
        val now = System.nanoTime()
        val dt = (now - lastFrameTime) / 1_000_000_000.0f
        lastFrameTime = now

        if (mousePressed) {
            particleSimulation.spawn(
                mouse.x / ZOOM,
                mouse.y / ZOOM)
        }
        particleSimulationImage = particleSimulation.step(dt)

        panel.repaint()

        title = "Kompute – Particle Simulation" +
                " | ${(1.0f / dt).toInt()} FPS" +
                " | ${particleSimulation.activeCount} particles | ${mouse.x}, ${mouse.y}"
    }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        add(panel)
        panel.background = Color.WHITE
        panel.preferredSize = Dimension(
            (ParticleSimulation.WIDTH * ZOOM).toInt(),
            (ParticleSimulation.HEIGHT * ZOOM).toInt()
        )
        pack()

        panel.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                mousePressed = true
            }

            override fun mouseReleased(event: MouseEvent) {
                mousePressed = false
            }
        })
        panel.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(event: MouseEvent) {
                mouse = event.point
            }
            override fun mouseDragged(event: MouseEvent) {
                mouse = event.point
            }
        })

        setLocationRelativeTo(null)
        isResizable = false
        isVisible = true
        loopTimer.start()
    }

    override fun dispose() {
        super.dispose()
        particleSimulation.close()
    }

    companion object {
        private const val ZOOM = 3.0f
    }
}

fun main() {
    SwingUtilities.invokeLater {
        ParticleSimulationFrame()
    }
}
