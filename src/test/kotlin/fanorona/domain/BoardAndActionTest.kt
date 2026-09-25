package fanorona.domain

import fanorona.setup.ClassicFanoronaFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BoardAndActionTest {
    private val board = Board()

    @Test
    fun `points expose directions and diagonal intersections`() {
        assertTrue(Point(0, 0).hasDiagonals())
        assertFalse(Point(1, 0).hasDiagonals())
        assertEquals(Direction.SE, Point(2, 1).directionTo(Point(3, 2)))
        assertEquals(Direction.W, Point(2, 1).directionTo(Point(0, 1)))
        assertNull(Point(2, 1).directionTo(Point(4, 2)))
        assertNull(Point(2, 1).directionTo(Point(2, 1)))
        assertEquals(Direction.NW, Direction.SE.opposite())
        assertEquals(Color.BLACK, Color.WHITE.opposite())
    }

    @Test
    fun `board follows strong and weak intersections`() {
        assertEquals(9, board.width)
        assertEquals(5, board.height)
        assertEquals(Point(8, 4), board.pointAt(8, 4))
        assertNull(board.pointAt(9, 4))
        assertNull(board.pointAt(0, -1))
        assertEquals(setOf(Point(1, 0), Point(0, 1), Point(1, 1)), board.neighbors(Point(0, 0)).toSet())
        assertEquals(setOf(Point(0, 0), Point(2, 0), Point(1, 1)), board.neighbors(Point(1, 0)).toSet())
        assertEquals(Point(1, 1), board.next(Point(0, 0), Direction.SE))
        assertNull(board.next(Point(1, 0), Direction.SW))
        assertNull(board.next(Point(8, 4), Direction.E))
    }

    @Test
    fun `arrangement moves and removes the same stones without sharing its copied positions`() {
        val white = Stone(Color.WHITE)
        val black = Stone(Color.BLACK)
        val arrangement = Arrangement(mapOf(Point(0, 0) to white, Point(2, 0) to black))
        val copy = arrangement.copy()

        assertSame(white, arrangement.stoneAt(Point(0, 0)))
        assertEquals(Point(0, 0), arrangement.pointOf(white))
        assertEquals(1, arrangement.count(Color.WHITE))
        assertEquals(1, arrangement.count(Color.BLACK))

        copy.move(white, Point(1, 0))
        copy.remove(listOf(black))

        assertEquals(Point(1, 0), copy.pointOf(white))
        assertNull(copy.stoneAt(Point(2, 0)))
        assertEquals(0, copy.count(Color.BLACK))
        assertEquals(Point(0, 0), arrangement.pointOf(white))
        assertSame(black, arrangement.stoneAt(Point(2, 0)))
    }

    @Test
    fun `classic setup has 22 stones per side and an empty centre`() {
        val factory = ClassicFanoronaFactory()
        val arrangement = factory.createInitialArrangement(factory.createBoard())

        assertEquals(22, arrangement.count(Color.WHITE))
        assertEquals(22, arrangement.count(Color.BLACK))
        assertNull(arrangement.stoneAt(Point(4, 2)))
        assertEquals(Color.BLACK, arrangement.stoneAt(Point(0, 0))?.color)
        assertEquals(Color.WHITE, arrangement.stoneAt(Point(8, 4))?.color)
        assertEquals(Color.BLACK, arrangement.stoneAt(Point(0, 2))?.color)
        assertEquals(Color.WHITE, arrangement.stoneAt(Point(1, 2))?.color)
    }

    @Test
    fun `paika moves without capturing`() {
        val stone = Stone(Color.WHITE)
        val arrangement = Arrangement(mapOf(Point(0, 0) to stone))

        assertEquals(emptyList(), Paika(stone, Point(1, 0)).apply(arrangement, board))
        assertEquals(Point(1, 0), arrangement.pointOf(stone))
    }

    @Test
    fun `approach captures a contiguous run and stops at own stone`() {
        val moving = Stone(Color.WHITE)
        val first = Stone(Color.BLACK)
        val second = Stone(Color.BLACK)
        val stop = Stone(Color.WHITE)
        val arrangement = Arrangement(
            mapOf(
                Point(0, 0) to moving,
                Point(2, 0) to first,
                Point(3, 0) to second,
                Point(4, 0) to stop,
            ),
        )

        val action = Approach(moving, Point(1, 0))
        assertEquals(listOf(first, second), action.captures(arrangement, board))
        assertEquals(listOf(first, second), action.apply(arrangement, board))
        assertEquals(Point(1, 0), arrangement.pointOf(moving))
        assertSame(stop, arrangement.stoneAt(Point(4, 0)))
        assertEquals(0, arrangement.count(Color.BLACK))
    }

    @Test
    fun `withdrawal captures behind origin and stops at empty point`() {
        val first = Stone(Color.BLACK)
        val second = Stone(Color.BLACK)
        val moving = Stone(Color.WHITE)
        val arrangement = Arrangement(
            mapOf(Point(0, 0) to first, Point(1, 0) to second, Point(2, 0) to moving),
        )

        val action = Withdrawal(moving, Point(3, 0))
        assertEquals(listOf(second, first), action.captures(arrangement, board))
        assertEquals(listOf(second, first), action.apply(arrangement, board))
        assertEquals(Point(3, 0), arrangement.pointOf(moving))
    }
}
