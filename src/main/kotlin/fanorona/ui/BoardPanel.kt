package fanorona.ui

import fanorona.domain.Board
import fanorona.domain.Color
import fanorona.domain.Direction
import fanorona.domain.Point
import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import kotlin.math.hypot
import kotlin.math.roundToInt
import java.awt.Color as AwtColor

class BoardPanel(private val onPointSelected: ((Point) -> Unit)? = null) : JPanel() {
    private var board = Board()
    private var stones = emptyMap<Point, Color>()
    private var selected: Point? = null
    private var movable = emptySet<Point>()
    private var targets = emptySet<Point>()

    init {
        preferredSize = Dimension(900, 500)
        minimumSize = Dimension(540, 300)
        background = AwtColor(0xF6F1E8)
        isFocusable = true
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                hitPoint(event.x, event.y)?.let { onPointSelected?.invoke(it) }
                requestFocusInWindow()
            }
        })
    }

    fun showBoard(
        board: Board,
        stones: Map<Point, Color>,
        selected: Point? = null,
        movable: Set<Point> = emptySet(),
        targets: Set<Point> = emptySet(),
    ) {
        this.board = board
        this.stones = stones.toMap()
        this.selected = selected
        this.movable = movable.toSet()
        this.targets = targets.toSet()
        repaint()
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val draw = graphics.create() as Graphics2D
        try {
            draw.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            draw.color = AwtColor(0xD8C6A7)
            draw.fillRoundRect(28, 28, width - 56, height - 56, 32, 32)
            draw.color = AwtColor(0x8B7355)
            draw.stroke = BasicStroke(2.5f)
            for (y in 0 until board.height) {
                for (x in 0 until board.width) {
                    val from = Point(x, y)
                    val (fromX, fromY) = center(from)
                    for (direction in listOf(Direction.E, Direction.SE, Direction.S, Direction.SW)) {
                        val to = board.next(from, direction) ?: continue
                        val (toX, toY) = center(to)
                        draw.drawLine(fromX.roundToInt(), fromY.roundToInt(), toX.roundToInt(), toY.roundToInt())
                    }
                }
            }

            val radius = (minOf(cellWidth(), cellHeight()) * 0.24).roundToInt().coerceAtLeast(10)
            draw.font = Font(Font.SANS_SERIF, Font.BOLD, 13)
            for (y in 0 until board.height) {
                for (x in 0 until board.width) {
                    val point = Point(x, y)
                    val (cx, cy) = center(point)
                    val px = cx.roundToInt()
                    val py = cy.roundToInt()
                    if (point in targets) {
                        draw.color = AwtColor(0x2E806E)
                        draw.fillOval(px - radius / 2, py - radius / 2, radius, radius)
                    } else {
                        draw.color = AwtColor(0x6A5540)
                        draw.fillOval(px - 4, py - 4, 8, 8)
                    }

                    val stone = stones[point] ?: continue
                    if (point == selected || point in movable) {
                        draw.color = if (point == selected) AwtColor(0x1F7A70) else AwtColor(0xBB843D)
                        draw.fillOval(px - radius - 6, py - radius - 6, 2 * radius + 12, 2 * radius + 12)
                    }
                    draw.color = AwtColor(0x6E5A44)
                    draw.fillOval(px - radius + 2, py - radius + 4, 2 * radius, 2 * radius)
                    draw.color = if (stone == Color.WHITE) AwtColor(0xF8F7F1) else AwtColor(0x24313E)
                    draw.fillOval(px - radius, py - radius, 2 * radius, 2 * radius)
                    draw.color = if (stone == Color.WHITE) AwtColor(0x5B554A) else AwtColor(0xD5DDE4)
                    draw.stroke = BasicStroke(2f)
                    draw.drawOval(px - radius, py - radius, 2 * radius, 2 * radius)
                }
            }

            draw.color = AwtColor(0x584C3C)
            for (x in 0 until board.width) {
                val (cx, _) = center(Point(x, 0))
                draw.drawString(('A' + x).toString(), cx.roundToInt() - 4, 22)
            }
            for (y in 0 until board.height) {
                val (_, cy) = center(Point(0, y))
                draw.drawString((y + 1).toString(), 12, cy.roundToInt() + 5)
            }
        } finally {
            draw.dispose()
        }
    }

    private fun hitPoint(x: Int, y: Int): Point? {
        val column = ((x - MARGIN) / cellWidth()).roundToInt()
        val row = ((y - MARGIN) / cellHeight()).roundToInt()
        val point = board.pointAt(column, row) ?: return null
        val (cx, cy) = center(point)
        val distance = hypot(x - cx, y - cy)
        return point.takeIf { distance <= minOf(cellWidth(), cellHeight()) * 0.29 }
    }

    private fun center(point: Point): Pair<Double, Double> =
        (MARGIN + point.x * cellWidth()) to (MARGIN + point.y * cellHeight())

    private fun cellWidth(): Double = (width - 2.0 * MARGIN) / (board.width - 1)

    private fun cellHeight(): Double = (height - 2.0 * MARGIN) / (board.height - 1)

    private companion object {
        const val MARGIN = 60.0
    }
}
