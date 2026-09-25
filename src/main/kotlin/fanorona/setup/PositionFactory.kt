package fanorona.setup

import fanorona.domain.Arrangement
import fanorona.domain.Board
import fanorona.domain.Color
import fanorona.domain.Point
import fanorona.domain.Stone
import fanorona.rules.ActionRule
import fanorona.rules.DrawRule

class PositionFactory(val description: String) : GameSetupFactory {
    private val classic = ClassicFanoronaFactory()

    override fun createBoard(): Board = classic.createBoard()

    override fun createInitialArrangement(board: Board): Arrangement {
        val rows = description.trim().lines()
        require(rows.size == board.height && rows.all { it.length == board.width }) {
            "Position must have ${board.height} rows of ${board.width} characters"
        }
        val positions = mutableMapOf<Point, Stone>()
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, symbol ->
                val color = when (symbol) {
                    'W' -> Color.WHITE
                    'B' -> Color.BLACK
                    '.' -> null
                    else -> throw IllegalArgumentException("Unknown position symbol: $symbol")
                }
                if (color != null) positions[Point(x, y)] = Stone(color)
            }
        }
        return Arrangement(positions)
    }

    override fun createDrawRules(): List<DrawRule> = classic.createDrawRules()

    override fun createActionRules(): List<ActionRule> = classic.createActionRules()
}
