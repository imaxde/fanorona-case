package fanorona.services

import fanorona.domain.Approach
import fanorona.domain.Color
import fanorona.domain.Outcome
import fanorona.domain.Paika
import fanorona.domain.Point
import fanorona.repositories.InMemoryGameRepository
import fanorona.repositories.InMemoryPlayerRepository
import fanorona.setup.ClassicFanoronaFactory
import fanorona.setup.GameSetupFactory
import fanorona.setup.PositionFactory
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GameServiceIntegrationTest {
    private val players = InMemoryPlayerRepository()
    private val games = InMemoryGameRepository()
    private val fixedClock = Clock.fixed(Instant.parse("2026-09-25T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `start creates missing players and rejects duplicate or simultaneous games`() {
        val service = service(ClassicFanoronaFactory())
        val notifications = mutableListOf<Int>()
        service.addListener(GameListener { notifications += it.arrangement.count(Color.WHITE) })

        assertFailsWith<IllegalArgumentException> { service.startGame(" Аня ", "аня") }
        assertTrue(players.findAll().isEmpty())
        val game = service.startGame(" Аня ", "Боб")

        assertEquals("Аня", game.whitePlayer.name)
        assertEquals("Боб", game.blackPlayer.name)
        assertEquals(LocalDate.of(2026, 9, 25), game.date)
        assertSame(game, service.currentGame())
        assertEquals(2, players.findAll().size)
        assertEquals(listOf(22), notifications)
        assertFailsWith<IllegalStateException> { service.startGame("Аня", "Вера") }
        assertEquals(2, players.findAll().size)
    }

    @Test
    fun `illegal action leaves arrangement and turn untouched`() {
        val service = service(ClassicFanoronaFactory())
        val game = service.startGame("Аня", "Боб")
        val before = game.arrangement.occupiedPoints()
        val moving = game.arrangement.stoneAt(Point(3, 2))!!
        val notifications = mutableListOf<String>()
        service.addListener(GameListener { notifications += "changed" })

        val violations = service.perform(Paika(moving, Point(4, 2)))

        assertTrue(violations.isNotEmpty())
        assertEquals(before, game.arrangement.occupiedPoints())
        assertEquals(Color.WHITE, game.currentSide)
        assertEquals(emptyList(), game.moves)
        assertNull(game.currentMove())
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun `capture of last stone archives game and notifies listeners`() {
        val service = service(PositionFactory("W.B......\n.........\n.........\n.........\n........."))
        val events = mutableListOf<Outcome?>()
        service.addListener(GameListener { events += it.outcome })
        val game = service.startGame("Аня", "Боб")
        val stone = game.arrangement.stoneAt(Point(0, 0))!!

        assertTrue(service.availableActions().any { it is Approach && it.target == Point(1, 0) })
        assertEquals(emptyList(), service.perform(Approach(stone, Point(1, 0))))

        assertEquals(Outcome.WHITE_WIN, game.outcome)
        assertEquals(listOf(null, Outcome.WHITE_WIN), events)
        assertNull(service.currentGame())
        assertTrue(service.availableActions().isEmpty())
        assertEquals(1L, game.id)
        assertSame(game, games.findById(1L))
        assertTrue(service.perform(Paika(stone, Point(0, 0))).isNotEmpty())
    }

    @Test
    fun `capture series may be stopped voluntarily`() {
        val factory = PositionFactory("...B.....\n.........\n..W.B....\n.........\n.........")
        val service = service(factory)
        val game = service.startGame("Аня", "Боб")
        val stone = game.arrangement.stoneAt(Point(2, 2))!!

        assertFailsWith<IllegalStateException> { service.endMove() }
        assertEquals(emptyList(), service.perform(Approach(stone, Point(3, 2))))
        assertTrue(service.availableActions().any { it is Approach && it.target == Point(3, 1) })
        assertEquals(1, game.currentMove()?.capturedCount())

        service.endMove()

        assertNull(game.currentMove())
        assertEquals(Color.BLACK, game.currentSide)
        assertEquals(1, game.moves.size)
        assertNull(game.outcome)
        assertFalse(service.availableActions().isEmpty())
    }

    @Test
    fun `cancel discards only the unfinished game and permits another start`() {
        val service = service(ClassicFanoronaFactory())
        service.startGame("Аня", "Боб")

        service.cancelGame()

        assertNull(service.currentGame())
        assertEquals(2, players.findAll().size)
        assertNull(games.findById(1))
        val next = service.startGame("Боб", "Аня")
        assertEquals("Боб", next.whitePlayer.name)
        assertEquals(2, players.findAll().size)
    }

    private fun service(factory: GameSetupFactory): GameService = GameService(
        players,
        games,
        ActionService(factory.createBoard(), factory.createActionRules()),
        factory,
        fixedClock,
    )
}
