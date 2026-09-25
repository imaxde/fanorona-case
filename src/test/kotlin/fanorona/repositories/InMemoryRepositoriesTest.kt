package fanorona.repositories

import fanorona.domain.Approach
import fanorona.domain.Arrangement
import fanorona.domain.Board
import fanorona.domain.Color
import fanorona.domain.Game
import fanorona.domain.Outcome
import fanorona.domain.Player
import fanorona.domain.Point
import fanorona.domain.Stone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class InMemoryRepositoriesTest {
    @Test
    fun `player repository stores unique names and finds them without case or surrounding spaces`() {
        val repository = InMemoryPlayerRepository()
        val player = Player("Алиса")

        repository.save(player)

        assertSame(player, repository.findByName(" алиса "))
        assertEquals(listOf(player), repository.findAll())
        assertNull(repository.findByName("Боб"))
        assertFailsWith<IllegalArgumentException> { repository.save(Player("АЛИСА")) }
    }

    @Test
    fun `game repository archives completed games with moves and stable identifiers`() {
        val repository = InMemoryGameRepository()
        val white = Player("Алиса")
        val black = Player("Боб")
        val moving = Stone(Color.WHITE)
        val game = Game(
            white,
            black,
            Board(),
            Arrangement(mapOf(Point(0, 0) to moving, Point(2, 0) to Stone(Color.BLACK))),
        )

        assertFailsWith<IllegalArgumentException> { repository.save(game) }
        game.apply(Approach(moving, Point(1, 0)))
        game.finishMove()
        repository.save(game)

        assertEquals(Outcome.WHITE_WIN, game.outcome)
        assertEquals(1L, game.id)
        assertSame(game, repository.findById(1L))
        assertNull(repository.findById(2L))
        assertEquals(listOf(game), repository.findByPlayer(white))
        assertEquals(listOf(game), repository.findByPlayer(black))
        assertEquals(emptyList(), repository.findByPlayer(Player("Другой")))
        assertEquals(game.moves, repository.findMoves(1L))
        assertEquals(emptyList(), repository.findMoves(2L))
        repository.save(game)
        assertEquals(listOf(game), repository.findByPlayer(white))
    }
}
