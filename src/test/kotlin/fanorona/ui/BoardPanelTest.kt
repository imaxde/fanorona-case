package fanorona.ui

import fanorona.domain.Board
import fanorona.domain.Color
import fanorona.domain.Point
import java.awt.Color as AwtColor
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardPanelTest {
    @Test
    fun `mouse click at an intersection selects the corresponding point`() {
        val clicked = mutableListOf<Point>()
        val panel = BoardPanel { clicked += it }
        panel.setSize(900, 500)
        panel.showBoard(Board(), mapOf(Point(0, 0) to Color.WHITE))

        panel.dispatchEvent(MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0, 0, 60, 60, 1, false))
        panel.dispatchEvent(MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0, 0, 840, 440, 1, false))
        panel.dispatchEvent(MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0, 0, 450, 30, 1, false))

        assertEquals(listOf(Point(0, 0), Point(8, 4)), clicked)
    }

    @Test
    fun `board paints with the current stones without changing game state`() {
        val panel = BoardPanel()
        panel.setSize(900, 500)
        panel.showBoard(
            Board(),
            mapOf(Point(0, 0) to Color.WHITE, Point(8, 4) to Color.BLACK),
            selected = Point(0, 0),
            targets = setOf(Point(1, 0)),
        )

        val image = BufferedImage(900, 500, BufferedImage.TYPE_INT_ARGB)
        panel.paint(image.createGraphics())

        val whitePixel = AwtColor(image.getRGB(60, 60), true)
        val blackPixel = AwtColor(image.getRGB(840, 440), true)
        assertTrue(whitePixel.red > blackPixel.red)
        assertTrue(whitePixel.green > blackPixel.green)
    }
}
