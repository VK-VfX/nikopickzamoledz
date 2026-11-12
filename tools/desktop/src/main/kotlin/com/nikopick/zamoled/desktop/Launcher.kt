package com.nikopick.zamoled.desktop

import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.File
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JWindow
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.system.exitProcess

/**
 * Starts the app. The wallpaper generators draw through Android's own graphics library, which is
 * brought up by Robolectric; that takes several seconds, so a small window says so meanwhile.
 */
object Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        val splash = JWindow().apply {
            contentPane.background = Color.BLACK
            contentPane.layout = GridBagLayout()
            val c = GridBagConstraints().apply { gridx = 0; fill = GridBagConstraints.HORIZONTAL }
            contentPane.add(JLabel("zamoled.", JLabel.CENTER).apply { foreground = Color.WHITE; font = Font("SansSerif", Font.BOLD, 40) }, c)
            contentPane.add(JLabel("Starting the renderer…", JLabel.CENTER).apply { foreground = Color(160, 160, 170); font = Font("SansSerif", Font.PLAIN, 14) }, c)
            size = Dimension(380, 170)
            setLocationRelativeTo(null)
        }
        SwingUtilities.invokeLater { splash.isVisible = true }
        Timer(150) { e ->
            if (System.getProperty("zamoled.ready") != null) {
                splash.dispose()
                (e.source as Timer).stop()
            }
        }.start()

        Thread({
            val result = org.junit.runner.JUnitCore().run(DesktopApp::class.java)
            if (!result.wasSuccessful()) {
                val why = result.failures.joinToString("\n\n") { it.trace.take(1500) }
                runCatching { File(System.getProperty("java.io.tmpdir"), "zamoled-error.txt").writeText(why) }
                SwingUtilities.invokeAndWait {
                    splash.dispose()
                    JOptionPane.showMessageDialog(null, "Zamoled could not start.\n\n" + why.take(700), "Zamoled", JOptionPane.ERROR_MESSAGE)
                }
            }
            exitProcess(0)
        }, "zamoled-main").start()
    }
}
