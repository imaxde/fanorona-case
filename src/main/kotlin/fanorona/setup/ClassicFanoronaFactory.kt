package fanorona.setup

import fanorona.domain.Arrangement
import fanorona.domain.Board
import fanorona.domain.Color
import fanorona.domain.Point
import fanorona.domain.Stone
import fanorona.rules.ActionRule
import fanorona.rules.AlongLineToFreeNeighborRule
import fanorona.rules.CaptureRemovesStoneRule
import fanorona.rules.DrawRule
import fanorona.rules.MandatoryCaptureRule
import fanorona.rules.NoCaptureLimitRule
import fanorona.rules.NoRepeatDirectionRule
import fanorona.rules.NoRevisitRule
import fanorona.rules.OwnStoneRule
import fanorona.rules.RepetitionRule
import fanorona.rules.SameStoneInSeriesRule
import fanorona.rules.SinglePaikaRule

class ClassicFanoronaFactory : GameSetupFactory {
    override fun createBoard(): Board = Board()

    override fun createInitialArrangement(board: Board): Arrangement {
        require(board.width == 9 && board.height == 5)
        val positions = mutableMapOf<Point, Stone>()
        for (y in 0 until board.height) {
            for (x in 0 until board.width) {
                val color = when (y) {
                    0, 1 -> Color.BLACK
                    3, 4 -> Color.WHITE
                    else -> when (x) {
                        0, 2, 5, 7 -> Color.BLACK
                        1, 3, 6, 8 -> Color.WHITE
                        else -> null
                    }
                }
                if (color != null) positions[Point(x, y)] = Stone(color)
            }
        }
        return Arrangement(positions)
    }

    override fun createDrawRules(): List<DrawRule> = listOf(
        NoCaptureLimitRule(),
        RepetitionRule(),
    )

    override fun createActionRules(): List<ActionRule> = listOf(
        OwnStoneRule(),
        AlongLineToFreeNeighborRule(),
        SameStoneInSeriesRule(),
        NoRevisitRule(),
        NoRepeatDirectionRule(),
        MandatoryCaptureRule(),
        SinglePaikaRule(),
        CaptureRemovesStoneRule(),
    )
}
