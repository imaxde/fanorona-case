package fanorona.domain

import fanorona.rules.NoCaptureLimitRule
import fanorona.rules.RepetitionRule
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GameTest {
    private val white = Player("Белые")
    private val black = Player("Чёрные")
    private val board = Board()

    @Test
    fun `new game starts with white and records the initial position`() {
        val initial = Arrangement(mapOf(Point(0, 0) to Stone(Color.WHITE), Point(8, 4) to Stone(Color.BLACK)))
        val date = LocalDate.of(2026, 9, 25)
        val game = Game(white, black, board, initial, emptyList(), date)

        assertEquals(date, game.date)
        assertEquals(Color.WHITE, game.currentSide)
        assertEquals(0, game.id)
        assertEquals(1, game.repetitionCount())
        assertEquals(0, game.movesWithoutCapture())
        assertEquals(emptyList(), game.moves)
        assertNull(game.currentMove())
        assertNull(game.outcome)
        assertEquals(initial.occupiedPoints(), game.arrangement.occupiedPoints())
    }

    @Test
    fun `finished paika changes the side and tracks its movement`() {
        val stone = Stone(Color.WHITE)
        val game = Game(
            white,
            black,
            board,
            Arrangement(mapOf(Point(0, 0) to stone, Point(8, 4) to Stone(Color.BLACK))),
            emptyList(),
        )

        game.startNextMove()
        assertEquals(0, game.apply(Paika(stone, Point(1, 0))))
        val current = game.currentMove()!!
        assertEquals(Color.WHITE, current.side)
        assertEquals(1, current.actions.size)
        assertEquals(listOf(Point(0, 0), Point(1, 0)), current.visitedPoints())
        assertEquals(Direction.E, current.lastDirection())
        assertEquals(0, current.capturedCount())
        assertEquals(Color.WHITE, game.currentSide)

        game.finishMove()

        assertNull(game.currentMove())
        assertEquals(Color.BLACK, game.currentSide)
        assertEquals(1, game.moves.size)
        assertEquals(1, game.movesWithoutCapture())
        assertEquals(Point(1, 0), game.arrangement.pointOf(stone))
        assertEquals(Point(0, 0), game.initialArrangement.pointOf(stone))
    }

    @Test
    fun `capturing the last enemy wins and resets the quiet move count`() {
        val moving = Stone(Color.WHITE)
        val enemy = Stone(Color.BLACK)
        val game = Game(
            white,
            black,
            board,
            Arrangement(mapOf(Point(0, 0) to moving, Point(2, 0) to enemy)),
            emptyList(),
        )

        game.apply(Approach(moving, Point(1, 0)))
        assertEquals(1, game.currentMove()?.capturedCount())
        assertFailsWith<IllegalStateException> { game.updateOutcome() }
        assertNull(game.outcome)
        game.finishMove()

        assertEquals(Outcome.WHITE_WIN, game.outcome)
        assertEquals(0, game.arrangement.count(Color.BLACK))
        assertEquals(0, game.movesWithoutCapture())
        assertEquals(1, game.moves.single().capturedCount())
        assertFailsWith<IllegalStateException> { game.startNextMove() }
    }

    @Test
    fun `forty completed quiet moves result in a draw`() {
        val rule = NoCaptureLimitRule()
        assertEquals(40, rule.limit)
        val game = alternatingGame(listOf(rule))

        repeat(40) { turn -> playAlternatingPaika(game, turn) }

        assertEquals(40, game.movesWithoutCapture())
        assertEquals(Outcome.DRAW, game.outcome)
    }

    @Test
    fun `third occurrence of arrangement and side results in a draw`() {
        val rule = RepetitionRule()
        assertEquals(3, rule.times)
        val game = alternatingGame(listOf(rule))

        repeat(8) { turn -> playAlternatingPaika(game, turn) }

        assertEquals(3, game.repetitionCount())
        assertEquals(Outcome.DRAW, game.outcome)
    }

    @Test
    fun `player names must not be blank and players must differ`() {
        assertFailsWith<IllegalArgumentException> { Player("  ") }
        assertFailsWith<IllegalArgumentException> {
            Game(white, Player("Белые"), board, Arrangement())
        }
    }

    private fun alternatingGame(drawRules: List<fanorona.rules.DrawRule>): Game = Game(
        white,
        black,
        board,
        Arrangement(mapOf(Point(0, 0) to Stone(Color.WHITE), Point(8, 4) to Stone(Color.BLACK))),
        drawRules,
    )

    private fun playAlternatingPaika(game: Game, turn: Int) {
        val (from, to) = when (turn % 4) {
            0 -> Point(0, 0) to Point(1, 0)
            1 -> Point(8, 4) to Point(7, 4)
            2 -> Point(1, 0) to Point(0, 0)
            else -> Point(7, 4) to Point(8, 4)
        }
        game.apply(Paika(game.arrangement.stoneAt(from)!!, to))
        game.finishMove()
    }
}
