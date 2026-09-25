package fanorona.ui

import fanorona.domain.Arrangement
import fanorona.domain.Board
import fanorona.domain.Color
import fanorona.domain.Point

object BoardRenderer {
    fun render(arrangement: Arrangement, board: Board): String = buildString {
        append("   ")
        append((0 until board.width).joinToString("   "))
        for (y in 0 until board.height) {
            appendLine()
            append(y)
            append("  ")
            append((0 until board.width).joinToString("---") { x ->
                when (arrangement.stoneAt(Point(x, y))?.color) {
                    Color.WHITE -> "W"
                    Color.BLACK -> "B"
                    null -> "."
                }
            })
            if (y < board.height - 1) {
                val links = CharArray((board.width - 1) * 4 + 1) { ' ' }
                for (x in 0 until board.width) {
                    links[x * 4] = '|'
                    if (Point(x, y).hasDiagonals()) {
                        if (x > 0) links[x * 4 - 2] = '/'
                        if (x < board.width - 1) links[x * 4 + 2] = '\\'
                    }
                }
                appendLine()
                append("   ")
                append(links.concatToString())
            }
        }
    }
}
