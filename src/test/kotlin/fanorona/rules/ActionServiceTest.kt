package fanorona.rules

import fanorona.domain.Action
import fanorona.domain.Approach
import fanorona.domain.Arrangement
import fanorona.domain.Board
import fanorona.domain.Capture
import fanorona.domain.Color
import fanorona.domain.Game
import fanorona.domain.Paika
import fanorona.domain.Player
import fanorona.domain.Point
import fanorona.domain.Stone
import fanorona.domain.Withdrawal
import fanorona.services.ActionService
import fanorona.setup.ClassicFanoronaFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActionServiceTest {
    private val board = Board()
    private val rules = ClassicFanoronaFactory().createActionRules()
    private val service = ActionService(board, rules)

    @Test
    fun `opening actions are all captures`() {
        val factory = ClassicFanoronaFactory()
        val game = Game(
            Player("A"),
            Player("B"),
            board,
            factory.createInitialArrangement(board),
        )

        val actions = service.availableActions(game)

        assertTrue(actions.isNotEmpty())
        assertTrue(actions.all { it is Capture })
        assertTrue(actions.all { service.validate(it, game).isEmpty() })
        assertFalse(service.canContinueMove(game))
    }

    @Test
    fun `paika is available only if there is no capture`() {
        val white = Stone(Color.WHITE)
        val game = game(mapOf(Point(0, 0) to white, Point(8, 4) to Stone(Color.BLACK)))

        val actions = service.availableActions(game)

        assertTrue(actions.isNotEmpty())
        assertTrue(actions.all { it is Paika })
        assertTrue(service.validate(Paika(white, Point(1, 0)), game).isEmpty())
        assertFalse(service.canContinueMove(game))
    }

    @Test
    fun `both capture types are offered when both remove enemies`() {
        val white = Stone(Color.WHITE)
        val target = Point(3, 2)
        val game = game(
            mapOf(
                Point(2, 2) to white,
                Point(1, 2) to Stone(Color.BLACK),
                Point(4, 2) to Stone(Color.BLACK),
            ),
        )

        val actions = service.availableActions(game).filter { it.stone === white && it.target == target }

        assertEquals(2, actions.size)
        assertTrue(actions.any { it is Approach })
        assertTrue(actions.any { it is Withdrawal })
        assertNotNull(MandatoryCaptureRule().check(Paika(white, target), game, board))
    }

    @Test
    fun `geometry and ownership rules reject invalid actions`() {
        val white = Stone(Color.WHITE)
        val black = Stone(Color.BLACK)
        val game = game(mapOf(Point(1, 0) to white, Point(2, 0) to black))

        assertNotNull(AlongLineToFreeNeighborRule().check(Paika(white, Point(0, 1)), game, board))
        assertNotNull(AlongLineToFreeNeighborRule().check(Paika(white, Point(2, 0)), game, board))
        assertNotNull(AlongLineToFreeNeighborRule().check(Paika(white, Point(4, 0)), game, board))
        assertNotNull(OwnStoneRule().check(Paika(black, Point(3, 0)), game, board))
        assertTrue(service.validate(Paika(white, Point(2, 0)), game).isNotEmpty())
        assertNull(OwnStoneRule().check(Paika(white, Point(1, 1)), game, board))
    }

    @Test
    fun `capture must actually remove an enemy`() {
        val white = Stone(Color.WHITE)
        val game = game(mapOf(Point(0, 0) to white, Point(8, 4) to Stone(Color.BLACK)))

        assertNotNull(CaptureRemovesStoneRule().check(Approach(white, Point(1, 0)), game, board))
        assertNotNull(CaptureRemovesStoneRule().check(Withdrawal(white, Point(1, 0)), game, board))
        assertTrue(service.validate(Approach(white, Point(1, 0)), game).isNotEmpty())
    }

    @Test
    fun `capture continuation uses one stone without revisiting or repeating direction`() {
        val moving = Stone(Color.WHITE)
        val other = Stone(Color.WHITE)
        val game = game(
            mapOf(
                Point(2, 2) to moving,
                Point(0, 0) to other,
                Point(4, 2) to Stone(Color.BLACK),
                Point(3, 0) to Stone(Color.BLACK),
            ),
        )
        game.apply(Approach(moving, Point(3, 2)))

        assertNotNull(SameStoneInSeriesRule().check(Paika(other, Point(1, 0)), game, board))
        assertNotNull(NoRevisitRule().check(Paika(moving, Point(2, 2)), game, board))
        assertNotNull(NoRepeatDirectionRule().check(Paika(moving, Point(4, 2)), game, board))
        assertNotNull(SinglePaikaRule().check(Paika(moving, Point(3, 1)), game, board))
        assertTrue(service.canContinueMove(game))
        assertTrue(service.availableActions(game).all { it is Capture && it.stone === moving })
        assertTrue(service.availableActions(game).any { it is Approach && it.target == Point(3, 1) })
    }

    @Test
    fun `paika cannot be followed by another action`() {
        val white = Stone(Color.WHITE)
        val game = game(mapOf(Point(0, 0) to white, Point(8, 4) to Stone(Color.BLACK)))
        game.apply(Paika(white, Point(1, 0)))

        assertTrue(service.availableActions(game).isEmpty())
        assertFalse(service.canContinueMove(game))
    }

    private fun game(positions: Map<Point, Stone>): Game =
        Game(Player("A"), Player("B"), board, Arrangement(positions))
}
