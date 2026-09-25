package fanorona.domain

import kotlin.math.abs
import kotlin.math.sign

data class Point(val x: Int, val y: Int) {
    fun hasDiagonals(): Boolean = (x + y) % 2 == 0

    fun directionTo(other: Point): Direction? {
        val dx = other.x - x
        val dy = other.y - y
        if (dx == 0 && dy == 0) return null
        if (dx != 0 && dy != 0 && abs(dx) != abs(dy)) return null
        return Direction.entries.firstOrNull {
            it.dx == dx.sign && it.dy == dy.sign
        }
    }
}
