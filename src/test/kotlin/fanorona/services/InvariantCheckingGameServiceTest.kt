package fanorona.services

import fanorona.domain.Approach
import fanorona.domain.Color
import fanorona.domain.Point
import fanorona.repositories.InMemoryGameRepository
import fanorona.repositories.InMemoryPlayerRepository
import fanorona.setup.PositionFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InvariantCheckingGameServiceTest {
    @Test
    fun `decorator delegates commands and verifies an ongoing capture series`() {
        val factory = PositionFactory("...B.....\n.........\n..W.B....\n.........\n.........")
        val base = GameService(
            InMemoryPlayerRepository(),
            InMemoryGameRepository(),
            ActionService(factory.createBoard(), factory.createActionRules()),
            factory,
        )
        val guarded: IGameService = InvariantCheckingGameService(base)
        val seen = mutableListOf<Color>()
        guarded.addListener(GameListener { seen += it.currentSide })

        val game = guarded.startGame("Аня", "Боб")
        assertSame(game, guarded.currentGame())
        assertTrue(guarded.availableActions().isNotEmpty())
        val stone = game.arrangement.stoneAt(Point(2, 2))!!
        assertEquals(emptyList(), guarded.perform(Approach(stone, Point(3, 2))))
        assertEquals(1, game.currentMove()?.capturedCount())

        guarded.endMove()
        assertEquals(Color.BLACK, game.currentSide)
        assertEquals(listOf(Color.WHITE, Color.WHITE, Color.BLACK), seen)

        guarded.cancelGame()
        assertNull(guarded.currentGame())
    }
}
