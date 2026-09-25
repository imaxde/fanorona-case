package fanorona.domain

class Board(val width: Int = 9, val height: Int = 5) {
    init {
        require(width > 0 && height > 0)
    }

    fun pointAt(x: Int, y: Int): Point? =
        if (x in 0 until width && y in 0 until height) Point(x, y) else null

    fun neighbors(point: Point): List<Point> =
        Direction.entries.mapNotNull { next(point, it) }

    fun next(point: Point, direction: Direction): Point? {
        if (pointAt(point.x, point.y) == null) return null
        if (direction.dx != 0 && direction.dy != 0 && !point.hasDiagonals()) return null
        return pointAt(point.x + direction.dx, point.y + direction.dy)
    }
}
