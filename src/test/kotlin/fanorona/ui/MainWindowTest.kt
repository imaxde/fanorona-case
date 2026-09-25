package fanorona.ui

import fanorona.main
import java.awt.Window
import java.nio.file.Files
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertTrue

class MainWindowTest {
    @Test
    fun `main launches the desktop window by default`() {
        val file = Files.createTempFile("fanorona-main-gui", ".db")
        main(arrayOf("--database=$file"))
        SwingUtilities.invokeAndWait {
            val windows = Window.getWindows().filterIsInstance<SwingApplication>().filter { it.isVisible }
            assertTrue(windows.isNotEmpty())
            windows.forEach(Window::dispose)
        }
    }
}
