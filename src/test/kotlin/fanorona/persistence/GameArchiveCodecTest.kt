package fanorona.persistence

import fanorona.domain.Approach
import fanorona.domain.Color
import fanorona.domain.Game
import fanorona.domain.Outcome
import fanorona.domain.Paika
import fanorona.domain.Player
import fanorona.domain.Point
import fanorona.setup.PositionFactory
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GameArchiveCodecTest {
    @Test
    fun `position and ordered actions reconstruct a finished game`() {
        val factory = PositionFactory("W.B.B....\n.........\n.........\n.........\n.........")
        val board = factory.createBoard()
        val original = Game(
            Player("Анна"), Player("Борис"), board,
            factory.createInitialArrangement(board), factory.createDrawRules(), LocalDate.of(2026, 9, 25),
        )
        val white = original.arrangement.stoneAt(Point(0, 0))!!
        val black = original.arrangement.stoneAt(Point(4, 0))!!
        original.apply(Approach(white, Point(1, 0)))
        original.finishMove()
        original.apply(Paika(black, Point(3, 0)))
        original.finishMove()
        original.apply(Approach(white, Point(2, 0)))
        original.finishMove()
        assertEquals(Outcome.WHITE_WIN, original.outcome)

        val position = GameArchiveCodec.encodePosition(board, original.initialArrangement)
        val moves = GameArchiveCodec.encodeMoves(original)
        val restored = GameArchiveCodec.restore(
            original.whitePlayer, original.blackPlayer, board,
            GameArchiveCodec.decodePosition(board, position), original.date, moves, checkNotNull(original.outcome),
        )

        assertEquals(original.initialArrangement.occupiedPoints(), restored.initialArrangement.occupiedPoints())
        assertEquals(original.arrangement.occupiedPoints(), restored.arrangement.occupiedPoints())
        assertEquals(original.moves.map { it.side }, restored.moves.map { it.side })
        assertEquals(original.moves.map { it.capturedCount() }, restored.moves.map { it.capturedCount() })
        assertEquals(original.outcome, restored.outcome)
        assertEquals(3, moves.size)
        assertEquals(listOf(StoredActionKind.APPROACH, StoredActionKind.PAIKA, StoredActionKind.APPROACH),
            moves.flatMap { it.actions }.map { it.kind })
    }

    @Test
    fun `invalid position and action sequence are rejected`() {
        val factory = PositionFactory("W.B......\n.........\n.........\n.........\n.........")
        val board = factory.createBoard()
        assertFailsWith<IllegalArgumentException> { GameArchiveCodec.decodePosition(board, "W") }
        assertFailsWith<IllegalArgumentException> {
            GameArchiveCodec.decodePosition(board, "X" + ".".repeat(board.width * board.height - 1))
        }
        assertFailsWith<IllegalArgumentException> {
            GameArchiveCodec.restore(
                Player("Анна"), Player("Борис"), board,
                factory.createInitialArrangement(board), LocalDate.of(2026, 9, 25),
                listOf(StoredMove(Color.BLACK, listOf(StoredAction(StoredActionKind.PAIKA, Point(0, 0), Point(1, 0))))),
                Outcome.WHITE_WIN,
            )
        }
    }
}
