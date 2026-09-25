package fanorona.persistence

import fanorona.domain.Approach
import fanorona.domain.Game
import fanorona.domain.Outcome
import fanorona.domain.Paika
import fanorona.domain.Player
import fanorona.domain.Point
import fanorona.domain.Withdrawal
import fanorona.rules.NoCaptureLimitRule
import fanorona.services.ReplayService
import fanorona.setup.PositionFactory
import java.nio.file.Files
import java.sql.SQLException
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqliteGameRepositoryTest {
    private val factory = PositionFactory("W.B.B....\n.........\n.........\n.........\n.........")
    private val anna = Player("Анна")
    private val boris = Player("Борис")

    @Test
    fun `finished games moves and statistics survive reopening`() {
        val file = Files.createTempFile("fanorona-games", ".db")
        SqliteDatabase(file).use { database ->
            val players = SqlitePlayerRepository(database)
            players.save(anna)
            players.save(boris)
            val games = SqliteGameRepository(database)
            val first = completedGame(anna, boris)
            val second = completedGame(boris, anna)
            games.save(first)
            games.save(second)
            games.save(first)
            assertEquals(1L, first.id)
            assertEquals(2L, second.id)
            assertEquals(2, games.statisticsFor(anna).games)
            assertEquals(1, games.statisticsFor(anna).wins)
            assertEquals(1, games.statisticsFor(anna).losses)
        }

        SqliteDatabase(file).use { database ->
            val players = SqlitePlayerRepository(database)
            val games = SqliteGameRepository(database)
            assertEquals(listOf(anna, boris), players.findAll())
            assertEquals(listOf(1L, 2L), games.findByPlayer(anna).map(Game::id))
            assertEquals(2, games.findByPlayer(boris).size)
            assertTrue(games.findByPlayer(Player("Неизвестный")).isEmpty())
            assertNull(games.findById(99))
            assertTrue(games.findMoves(99).isEmpty())
            val restored = games.findById(1)!!
            assertEquals(LocalDate.of(2026, 9, 25), restored.date)
            assertEquals(Outcome.WHITE_WIN, restored.outcome)
            assertEquals(3, games.findMoves(1).size)
            assertEquals(1, games.statisticsFor(anna).wins)
            assertEquals(1, games.statisticsFor(boris).wins)
            assertEquals(0, games.statisticsFor(Player("Неизвестный")).games)

            val replay = ReplayService(games, factory)
            replay.load(1)
            assertEquals(3, replay.currentArrangement().occupiedPoints().size)
            repeat(3) { assertTrue(replay.stepForward()) }
            assertEquals(restored.arrangement.occupiedPoints(), replay.currentArrangement().occupiedPoints())
        }
    }

    @Test
    fun `failed action insert rolls back game and player statistics`() {
        val file = Files.createTempFile("fanorona-game-rollback", ".db")
        SqliteDatabase(file).use { database ->
            val players = SqlitePlayerRepository(database)
            players.save(anna)
            players.save(boris)
            database.connection.createStatement().use {
                it.execute("CREATE TRIGGER fail_action BEFORE INSERT ON actions BEGIN SELECT RAISE(ABORT, 'forced'); END")
            }
            val games = SqliteGameRepository(database)
            val game = completedGame(anna, boris)
            assertFailsWith<SQLException> { games.save(game) }
            assertEquals(0L, game.id)
            assertNull(games.findById(1))
            assertEquals(0, games.statisticsFor(anna).games)
            assertEquals(0, games.statisticsFor(boris).games)
            assertFailsWith<IllegalArgumentException> { games.save(completedGame(Player("Незнакомец"), boris)) }
        }
    }

    @Test
    fun `black victory updates the correct player records`() {
        val localFactory = PositionFactory("W.B......\n.........\n.........\n.........\n.........")
        val file = Files.createTempFile("fanorona-black-win", ".db")
        SqliteDatabase(file).use { database ->
            val players = SqlitePlayerRepository(database)
            players.save(anna)
            players.save(boris)
            val board = localFactory.createBoard()
            val game = Game(anna, boris, board, localFactory.createInitialArrangement(board),
                localFactory.createDrawRules(), LocalDate.of(2026, 9, 25))
            val white = game.arrangement.stoneAt(Point(0, 0))!!
            val black = game.arrangement.stoneAt(Point(2, 0))!!
            game.apply(Paika(white, Point(1, 0)))
            game.finishMove()
            game.apply(Withdrawal(black, Point(3, 0)))
            game.finishMove()
            assertEquals(Outcome.BLACK_WIN, game.outcome)

            val games = SqliteGameRepository(database)
            games.save(game)
            assertEquals(1, games.statisticsFor(boris).wins)
            assertEquals(1, games.statisticsFor(anna).losses)
            assertEquals(Outcome.BLACK_WIN, games.findById(game.id)?.outcome)
        }
    }

    @Test
    fun `draw is persisted as a played game without a win or loss`() {
        val position = PositionFactory("W.......B\n.........\n.........\n.........\n.........")
        val drawFactory = object : fanorona.setup.GameSetupFactory by position {
            override fun createDrawRules(): List<fanorona.rules.DrawRule> = listOf(NoCaptureLimitRule(1))
        }
        val file = Files.createTempFile("fanorona-draw", ".db")
        SqliteDatabase(file).use { database ->
            val players = SqlitePlayerRepository(database)
            players.save(anna)
            players.save(boris)
            val board = drawFactory.createBoard()
            val game = Game(anna, boris, board, drawFactory.createInitialArrangement(board),
                drawFactory.createDrawRules(), LocalDate.of(2026, 9, 25))
            game.apply(Paika(game.arrangement.stoneAt(Point(0, 0))!!, Point(1, 0)))
            game.finishMove()
            assertEquals(Outcome.DRAW, game.outcome)

            val games = SqliteGameRepository(database)
            games.save(game)
            assertEquals(1, games.statisticsFor(anna).draws)
            assertEquals(1, games.statisticsFor(boris).draws)
            assertEquals(Outcome.DRAW, games.findById(game.id)?.outcome)
        }
        SqliteDatabase(file).use { database ->
            val games = SqliteGameRepository(database)
            assertEquals(Outcome.DRAW, games.findById(1)?.outcome)
        }
    }

    private fun completedGame(white: Player, black: Player): Game {
        val board = factory.createBoard()
        val game = Game(white, black, board, factory.createInitialArrangement(board),
            factory.createDrawRules(), LocalDate.of(2026, 9, 25))
        val whiteStone = game.arrangement.stoneAt(Point(0, 0))!!
        val blackStone = game.arrangement.stoneAt(Point(4, 0))!!
        game.apply(Approach(whiteStone, Point(1, 0)))
        game.finishMove()
        game.apply(Paika(blackStone, Point(3, 0)))
        game.finishMove()
        game.apply(Approach(whiteStone, Point(2, 0)))
        game.finishMove()
        return game
    }
}
