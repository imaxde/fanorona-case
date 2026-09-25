package fanorona.rules

import fanorona.domain.Action
import fanorona.domain.Approach
import fanorona.domain.Board
import fanorona.domain.Capture
import fanorona.domain.Game
import fanorona.domain.Paika
import fanorona.domain.Withdrawal

data class Violation(val message: String)

fun interface ActionRule {
    fun check(action: Action, game: Game, board: Board): Violation?
}

class OwnStoneRule : ActionRule {
    override fun check(action: Action, game: Game, board: Board): Violation? =
        if (action.stone.color != game.currentSide) Violation("Сейчас ход другой стороны") else null
}

class AlongLineToFreeNeighborRule : ActionRule {
    override fun check(action: Action, game: Game, board: Board): Violation? {
        val arrangement = game.arrangement
        val origin = arrangement.pointOf(action.stone)
            ?: return Violation("Камня нет на доске")
        val direction = origin.directionTo(action.target)
            ?: return Violation("Точки не лежат на одной линии")
        if (board.next(origin, direction) != action.target) {
            return Violation("Можно перейти только в соседнюю точку по линии")
        }
        if (arrangement.stoneAt(action.target) != null) {
            return Violation("Целевая точка занята")
        }
        return null
    }
}

class SameStoneInSeriesRule : ActionRule {
    override fun check(action: Action, game: Game, board: Board): Violation? {
        val first = game.currentMove()?.actions?.firstOrNull() ?: return null
        return if (first.stone !== action.stone) {
            Violation("Серию захватов продолжает тот же камень")
        } else {
            null
        }
    }
}

class NoRevisitRule : ActionRule {
    override fun check(action: Action, game: Game, board: Board): Violation? =
        if (action.target in (game.currentMove()?.visitedPoints() ?: emptyList())) {
            Violation("В течение хода нельзя повторно посещать точку")
        } else {
            null
        }
}

class NoRepeatDirectionRule : ActionRule {
    override fun check(action: Action, game: Game, board: Board): Violation? {
        val previous = game.currentMove()?.lastDirection() ?: return null
        return if (previous == action.direction(game.arrangement)) {
            Violation("Нельзя дважды подряд двигаться в одном направлении")
        } else {
            null
        }
    }
}

class MandatoryCaptureRule : ActionRule {
    override fun check(action: Action, game: Game, board: Board): Violation? {
        if (action !is Paika || game.currentMove()?.actions?.isNotEmpty() == true) return null
        val arrangement = game.arrangement
        for ((point, color) in arrangement.occupiedPoints()) {
            if (color != game.currentSide) continue
            val stone = checkNotNull(arrangement.stoneAt(point))
            for (target in board.neighbors(point)) {
                if (arrangement.stoneAt(target) != null) continue
                if (Approach(stone, target).captures(arrangement, board).isNotEmpty() ||
                    Withdrawal(stone, target).captures(arrangement, board).isNotEmpty()
                ) {
                    return Violation("Доступен обязательный захват")
                }
            }
        }
        return null
    }
}

class SinglePaikaRule : ActionRule {
    override fun check(action: Action, game: Game, board: Board): Violation? {
        val first = game.currentMove()?.actions?.firstOrNull() ?: return null
        return if (first is Paika || action is Paika) {
            Violation("Пайка не входит в серию действий")
        } else {
            null
        }
    }
}

class CaptureRemovesStoneRule : ActionRule {
    override fun check(action: Action, game: Game, board: Board): Violation? =
        if (action is Capture && action.captures(game.arrangement, board).isEmpty()) {
            Violation("Захват должен снять хотя бы один камень")
        } else {
            null
        }
}
