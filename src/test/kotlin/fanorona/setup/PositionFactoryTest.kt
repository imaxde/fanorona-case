package fanorona.setup

import fanorona.domain.Color
import fanorona.domain.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PositionFactoryTest {
    @Test
    fun `position factory creates a specified arrangement with classic rules`() {
        val factory = PositionFactory("W.B......\n.........\n.........\n.........\n.........")
        val board = factory.createBoard()
        val arrangement = factory.createInitialArrangement(board)

        assertTrue(factory.description.startsWith("W.B"))
        assertEquals(1, arrangement.count(Color.WHITE))
        assertEquals(1, arrangement.count(Color.BLACK))
        assertEquals(Color.WHITE, arrangement.stoneAt(Point(0, 0))?.color)
        assertEquals(2, factory.createDrawRules().size)
        assertTrue(factory.createActionRules().isNotEmpty())
    }

    @Test
    fun `position factory rejects malformed descriptions`() {
        assertFailsWith<IllegalArgumentException> { PositionFactory("bad").createInitialArrangement(PositionFactory("bad").createBoard()) }
        assertFailsWith<IllegalArgumentException> {
            PositionFactory("W.X......\n.........\n.........\n.........\n.........").createInitialArrangement(
                PositionFactory("W.X......\n.........\n.........\n.........\n.........").createBoard(),
            )
        }
    }
}
