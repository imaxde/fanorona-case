package fanorona.services

import fanorona.domain.Action
import fanorona.domain.Approach
import fanorona.domain.Board
import fanorona.domain.Capture
import fanorona.domain.Game
import fanorona.domain.Paika
import fanorona.domain.Withdrawal
import fanorona.rules.ActionRule
import fanorona.rules.AlongLineToFreeNeighborRule
import fanorona.rules.OwnStoneRule
import fanorona.rules.Violation

class ActionService(
    private val board: Board,
    private val rules: List<ActionRule>,
) {
    fun validate(action: Action, game: Game): List<Violation> {
        if (game.outcome != null) return listOf(Violation("Партия завершена"))
        val basic = listOfNotNull(
            OwnStoneRule().check(action, game, board),
            AlongLineToFreeNeighborRule().check(action, game, board),
        )
        if (basic.isNotEmpty()) return basic
        return rules.mapNotNull { it.check(action, game, board) }
    }

    fun availableActions(game: Game): List<Action> {
        if (game.outcome != null) return emptyList()
        val current = game.currentMove()
        if (current?.actions?.firstOrNull() is Paika) return emptyList()
        val arrangement = game.arrangement
        val stones = if (current?.actions?.isNotEmpty() == true) {
            listOf(current.actions.first().stone)
        } else {
            arrangement.occupiedPoints().filterValues { it == game.currentSide }
                .keys.mapNotNull { arrangement.stoneAt(it) }
        }
        return buildList {
            for (stone in stones) {
                val origin = arrangement.pointOf(stone) ?: continue
                for (target in board.neighbors(origin)) {
                    if (arrangement.stoneAt(target) != null) continue
                    val candidates = listOf(
                        Paika(stone, target),
                        Approach(stone, target),
                        Withdrawal(stone, target),
                    )
                    addAll(candidates.filter { validate(it, game).isEmpty() })
                }
            }
        }
    }

    fun canContinueMove(game: Game): Boolean =
        game.currentMove()?.actions?.lastOrNull() is Capture &&
            availableActions(game).any { it is Capture }
}
