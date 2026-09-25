package fanorona.ui

import fanorona.domain.Board
import fanorona.domain.Color
import fanorona.domain.Game
import fanorona.domain.Outcome
import fanorona.domain.Player
import fanorona.domain.Point
import fanorona.domain.PlayerStatistics

enum class ActionKind { PAIKA, APPROACH, WITHDRAWAL }

data class ActionOption(val kind: ActionKind, val captured: Int)

sealed interface BoardClickResult {
    data object SelectionChanged : BoardClickResult
    data object ActionApplied : BoardClickResult
    data class ChooseAction(val options: List<ActionOption>) : BoardClickResult
    data class Invalid(val message: String) : BoardClickResult
}

data class BoardState(
    val board: Board,
    val stones: Map<Point, Color>,
    val active: Boolean,
    val selected: Point?,
    val movable: Set<Point>,
    val targets: Set<Point>,
    val whiteName: String?,
    val blackName: String?,
    val currentSide: Color?,
    val whiteCount: Int,
    val blackCount: Int,
    val capturedThisMove: Int,
    val movesWithoutCapture: Int,
    val canEndMove: Boolean,
    val outcome: Outcome?,
)

data class ReplayState(
    val game: Game,
    val stones: Map<Point, Color>,
    val step: Int,
    val totalSteps: Int,
)

data class PlayerSummary(
    val player: Player,
    val statistics: PlayerStatistics,
    val history: List<Game>,
)
