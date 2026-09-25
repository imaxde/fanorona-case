package fanorona.ui

import fanorona.domain.Color
import fanorona.domain.Action
import fanorona.domain.Point
import fanorona.repositories.InMemoryGameRepository
import fanorona.repositories.InMemoryPlayerRepository
import fanorona.services.ActionService
import fanorona.services.GameService
import fanorona.services.GameServiceDecorator
import fanorona.services.PlayerRegistryService
import fanorona.services.ReplayService
import fanorona.services.StatisticsService
import fanorona.setup.GameSetupFactory
import fanorona.setup.PositionFactory
import fanorona.rules.Violation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopControllerTest {
    @Test
    fun `board selection offers both capture types and applies chosen type`() {
        val controller = controller(PositionFactory(".BW.B....\n.........\n.........\n.........\n........."))
        controller.registerPlayer("Анна")
        controller.registerPlayer("Борис")
        controller.startGame("Анна", "Борис")

        assertEquals(BoardClickResult.SelectionChanged, controller.clickBoard(Point(2, 0)))
        val choice = assertIs<BoardClickResult.ChooseAction>(controller.clickBoard(Point(3, 0)))
        assertEquals(setOf(ActionKind.APPROACH, ActionKind.WITHDRAWAL), choice.options.map { it.kind }.toSet())

        assertEquals(BoardClickResult.ActionApplied, controller.chooseAction(ActionKind.WITHDRAWAL))
        val board = controller.boardState()
        assertEquals(Color.WHITE, board.stones[Point(3, 0)])
        assertFalse(Point(1, 0) in board.stones)
        assertEquals(Color.BLACK, board.stones[Point(4, 0)])
    }

    @Test
    fun `completed GUI game updates statistics and replay`() {
        val controller = controller(PositionFactory("W.B......\n.........\n.........\n.........\n........."))
        controller.registerPlayer("Анна")
        controller.registerPlayer("Борис")
        controller.startGame("Анна", "Борис")

        assertEquals(BoardClickResult.SelectionChanged, controller.clickBoard(Point(0, 0)))
        assertEquals(BoardClickResult.ActionApplied, controller.clickBoard(Point(1, 0)))
        assertFalse(controller.boardState().active)
        assertEquals(1, controller.statistics("Анна").wins)

        val finished = controller.history("Анна").single()
        controller.loadReplay(finished)
        assertEquals(1, controller.replayState()!!.stones.values.count { it == Color.BLACK })
        assertTrue(controller.replayNext())
        assertEquals(0, controller.replayState()!!.stones.values.count { it == Color.BLACK })
        assertFalse(controller.replayNext())
        assertTrue(controller.replayBack())
    }

    @Test
    fun `rejected action keeps selection and reports service violation`() {
        val factory = PositionFactory("W.B......\n.........\n.........\n.........\n.........")
        val players = InMemoryPlayerRepository()
        val games = InMemoryGameRepository()
        val service = GameService(
            players,
            games,
            ActionService(factory.createBoard(), factory.createActionRules()),
            factory,
        )
        val rejecting = object : GameServiceDecorator(service) {
            override fun perform(action: Action): List<Violation> = listOf(Violation("Ход отклонён"))
        }
        val controller = DesktopController(
            rejecting,
            PlayerRegistryService(players),
            StatisticsService(players, games),
            ReplayService(games, factory),
            factory,
        )
        controller.registerPlayer("Анна")
        controller.registerPlayer("Борис")
        controller.startGame("Анна", "Борис")

        controller.clickBoard(Point(0, 0))
        val result = controller.clickBoard(Point(1, 0))

        assertEquals(BoardClickResult.Invalid("Ход отклонён"), result)
        assertEquals(Point(0, 0), controller.boardState().selected)
        assertEquals(Color.BLACK, controller.boardState().stones[Point(2, 0)])
    }

    private fun controller(factory: GameSetupFactory): DesktopController {
        val players = InMemoryPlayerRepository()
        val games = InMemoryGameRepository()
        return DesktopController(
            GameService(
                players,
                games,
                ActionService(factory.createBoard(), factory.createActionRules()),
                factory,
            ),
            PlayerRegistryService(players),
            StatisticsService(players, games),
            ReplayService(games, factory),
            factory,
        )
    }
}
