package fanorona.persistence

import fanorona.domain.Action
import fanorona.domain.Approach
import fanorona.domain.Arrangement
import fanorona.domain.Board
import fanorona.domain.Color
import fanorona.domain.Game
import fanorona.domain.Outcome
import fanorona.domain.Paika
import fanorona.domain.Player
import fanorona.domain.Point
import fanorona.domain.Stone
import fanorona.domain.Withdrawal
import java.time.LocalDate

internal enum class StoredActionKind { PAIKA, APPROACH, WITHDRAWAL }

internal data class StoredAction(val kind: StoredActionKind, val from: Point, val to: Point)

internal data class StoredMove(val side: Color, val actions: List<StoredAction>)

/** Converts a completed game to ordered records and restores its domain state by replaying them. */
internal object GameArchiveCodec {
    fun encodePosition(board: Board, arrangement: Arrangement): String = buildString {
        for (y in 0 until board.height) {
            for (x in 0 until board.width) {
                append(when (arrangement.stoneAt(Point(x, y))?.color) {
                    Color.WHITE -> 'W'
                    Color.BLACK -> 'B'
                    null -> '.'
                })
            }
        }
    }

    fun decodePosition(board: Board, encoded: String): Arrangement {
        require(encoded.length == board.width * board.height) { "Invalid saved position size" }
        val stones = buildMap {
            encoded.forEachIndexed { index, symbol ->
                val color = when (symbol) {
                    'W' -> Color.WHITE
                    'B' -> Color.BLACK
                    '.' -> null
                    else -> throw IllegalArgumentException("Invalid saved position symbol")
                }
                if (color != null) put(Point(index % board.width, index / board.width), Stone(color))
            }
        }
        return Arrangement(stones)
    }

    fun encodeMoves(game: Game): List<StoredMove> {
        val position = game.initialArrangement
        val moves = game.moves.map { move ->
            StoredMove(move.side, move.actions.map { action ->
                val from = requireNotNull(position.pointOf(action.stone)) { "Archived stone is missing" }
                val kind = when (action) {
                    is Paika -> StoredActionKind.PAIKA
                    is Approach -> StoredActionKind.APPROACH
                    is Withdrawal -> StoredActionKind.WITHDRAWAL
                }
                action.apply(position, game.board)
                StoredAction(kind, from, action.target)
            })
        }
        require(position.occupiedPoints() == game.arrangement.occupiedPoints()) {
            "Archived actions do not match the final position"
        }
        return moves
    }

    fun restore(
        white: Player,
        black: Player,
        board: Board,
        initial: Arrangement,
        date: LocalDate,
        moves: List<StoredMove>,
        outcome: Outcome,
    ): Game {
        val game = Game(white, black, board, initial, emptyList(), date)
        for (move in moves) {
            require(move.side == game.currentSide && move.actions.isNotEmpty()) { "Invalid saved move" }
            for (record in move.actions) {
                val stone = requireNotNull(game.arrangement.stoneAt(record.from)) { "Saved stone is missing" }
                val action: Action = when (record.kind) {
                    StoredActionKind.PAIKA -> Paika(stone, record.to)
                    StoredActionKind.APPROACH -> Approach(stone, record.to)
                    StoredActionKind.WITHDRAWAL -> Withdrawal(stone, record.to)
                }
                game.apply(action)
            }
            game.finishMove()
        }
        game.restoreOutcome(outcome)
        return game
    }
}
