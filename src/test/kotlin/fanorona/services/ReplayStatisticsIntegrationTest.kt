package fanorona.services

import fanorona.domain.Approach
import fanorona.domain.Color
import fanorona.domain.Outcome
import fanorona.domain.Paika
import fanorona.domain.Player
import fanorona.domain.PlayerStatistics
import fanorona.domain.Point
import fanorona.repositories.InMemoryGameRepository
import fanorona.repositories.InMemoryPlayerRepository
import fanorona.setup.PositionFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReplayStatisticsIntegrationTest {
    @Test
    fun `completed game contributes to statistics and can be replayed in both directions`() {
        val factory = PositionFactory("W.B......\n.........\n.........\n.........\n.........")
        val players = InMemoryPlayerRepository()
        val games = InMemoryGameRepository()
        val gameService = GameService(
            players,
            games,
            ActionService(factory.createBoard(), factory.createActionRules()),
            factory,
        )
        val game = gameService.startGame("Аня", "Боб")
        gameService.perform(Approach(game.arrangement.stoneAt(Point(0, 0))!!, Point(1, 0)))

        val statistics = StatisticsService(players, games)
        assertEquals(listOf("Аня", "Боб"), statistics.players().map { it.name })
        assertEquals(listOf(game), statistics.gamesOf(Player("Аня")))
        assertEquals(1, statistics.statisticsFor(Player("Аня")).wins)
        assertEquals(1, statistics.statisticsFor(Player("аня")).wins)
        assertEquals(1, statistics.statisticsFor(Player("Боб")).losses)
        assertEquals(0, statistics.statisticsFor(Player("Аня")).draws)
        assertEquals(1, statistics.statisticsFor(Player("Аня")).games)
        assertEquals(null, statistics.findPlayer("Неизвестный"))
        assertEquals(Player("Аня"), statistics.findPlayer("аня"))

        val replay = ReplayService(games, factory)
        assertFailsWith<IllegalStateException> { replay.currentArrangement() }
        assertFailsWith<IllegalArgumentException> { replay.load(999) }
        replay.load(game.id)
        assertEquals(1, replay.currentArrangement().count(Color.BLACK))
        assertTrue(replay.stepForward())
        assertEquals(0, replay.currentArrangement().count(Color.BLACK))
        assertFalse(replay.stepForward())
        assertTrue(replay.stepBack())
        assertEquals(1, replay.currentArrangement().count(Color.BLACK))
        assertFalse(replay.stepBack())
    }

    @Test
    fun `statistics starts at zero for a player without completed games`() {
        val players = InMemoryPlayerRepository()
        val games = InMemoryGameRepository()
        val player = Player("Аня")
        players.save(player)
        val service = StatisticsService(players, games)

        assertEquals(emptyList(), service.gamesOf(player))
        assertEquals(PlayerStatistics(0, 0, 0), service.statisticsFor(player))
        assertEquals(0, service.statisticsFor(player).draws)
    }

    @Test
    fun `replay applies every action in one capture move`() {
        val factory = PositionFactory("...B.....\n.........\n..W.B....\n.........\n.........")
        val players = InMemoryPlayerRepository()
        val games = InMemoryGameRepository()
        val service = GameService(
            players,
            games,
            ActionService(factory.createBoard(), factory.createActionRules()),
            factory,
        )
        val game = service.startGame("Аня", "Боб")
        val stone = game.arrangement.stoneAt(Point(2, 2))!!

        service.perform(Approach(stone, Point(3, 2)))
        service.perform(Approach(stone, Point(3, 1)))

        assertEquals(Outcome.WHITE_WIN, game.outcome)
        assertEquals(2, game.moves.single().actions.size)
        assertEquals(2, game.moves.single().capturedCount())
        val replay = ReplayService(games, factory)
        replay.load(game.id)
        assertEquals(2, replay.currentArrangement().count(Color.BLACK))
        assertTrue(replay.stepForward())
        assertEquals(0, replay.currentArrangement().count(Color.BLACK))
        assertEquals(Point(3, 1), replay.currentArrangement().pointOf(stone))
    }

    @Test
    fun `repetition draw is archived and counted for both players`() {
        val factory = PositionFactory("W........\n.........\n.........\n.........\n........B")
        val players = InMemoryPlayerRepository()
        val games = InMemoryGameRepository()
        val service = GameService(
            players,
            games,
            ActionService(factory.createBoard(), factory.createActionRules()),
            factory,
        )
        val game = service.startGame("Аня", "Боб")
        val steps = listOf(
            Point(0, 0) to Point(1, 0),
            Point(8, 4) to Point(7, 4),
            Point(1, 0) to Point(0, 0),
            Point(7, 4) to Point(8, 4),
        )
        repeat(8) { turn ->
            val (from, to) = steps[turn % steps.size]
            assertEquals(emptyList(), service.perform(Paika(game.arrangement.stoneAt(from)!!, to)))
        }

        assertEquals(Outcome.DRAW, game.outcome)
        assertEquals(3, game.repetitionCount())
        assertEquals(1L, game.id)
        val statistics = StatisticsService(players, games)
        assertEquals(1, statistics.statisticsFor(Player("Аня")).draws)
        assertEquals(1, statistics.statisticsFor(Player("Боб")).draws)
    }

    @Test
    fun `new game resets the board and reuses players with reversed colors`() {
        val factory = PositionFactory("W.B......\n.........\n.........\n.........\n.........")
        val players = InMemoryPlayerRepository()
        val games = InMemoryGameRepository()
        val service = GameService(
            players,
            games,
            ActionService(factory.createBoard(), factory.createActionRules()),
            factory,
        )
        val first = service.startGame("Аня", "Боб")
        service.perform(Approach(first.arrangement.stoneAt(Point(0, 0))!!, Point(1, 0)))

        val second = service.startGame("БОБ", "аня")
        assertEquals(1, second.arrangement.count(Color.BLACK))
        assertEquals(Point(0, 0), second.arrangement.pointOf(second.arrangement.stoneAt(Point(0, 0))!!))
        service.perform(Approach(second.arrangement.stoneAt(Point(0, 0))!!, Point(1, 0)))

        assertEquals(2, players.findAll().size)
        assertEquals(1L, first.id)
        assertEquals(2L, second.id)
        val statistics = StatisticsService(players, games)
        assertEquals(2, statistics.statisticsFor(Player("Аня")).games)
        assertEquals(1, statistics.statisticsFor(Player("Аня")).wins)
        assertEquals(1, statistics.statisticsFor(Player("Аня")).losses)
        assertEquals(1, statistics.statisticsFor(Player("Боб")).wins)
    }
}
