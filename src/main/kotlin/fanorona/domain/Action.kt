package fanorona.domain

sealed class Action(val stone: Stone, val target: Point) {
    fun direction(arrangement: Arrangement): Direction? =
        arrangement.pointOf(stone)?.directionTo(target)

    protected fun requireOrigin(arrangement: Arrangement, board: Board): Pair<Point, Direction> {
        val origin = requireNotNull(arrangement.pointOf(stone)) { "Stone is not on the board" }
        val direction = requireNotNull(origin.directionTo(target)) { "Points are not in one direction" }
        require(board.next(origin, direction) == target) { "Destination is not a neighboring point" }
        require(arrangement.stoneAt(target) == null) { "Destination is occupied" }
        return origin to direction
    }

    abstract fun apply(arrangement: Arrangement, board: Board): List<Stone>
}

class Paika(stone: Stone, target: Point) : Action(stone, target) {
    override fun apply(arrangement: Arrangement, board: Board): List<Stone> {
        requireOrigin(arrangement, board)
        arrangement.move(stone, target)
        return emptyList()
    }
}

sealed class Capture(stone: Stone, target: Point) : Action(stone, target) {
    fun captures(arrangement: Arrangement, board: Board): List<Stone> {
        val (origin, direction) = requireOrigin(arrangement, board)
        return capturedStones(origin, direction, arrangement, board)
    }

    protected abstract fun capturedStones(
        from: Point,
        direction: Direction,
        arrangement: Arrangement,
        board: Board,
    ): List<Stone>

    protected fun contiguousEnemies(
        start: Point?,
        direction: Direction,
        arrangement: Arrangement,
        board: Board,
    ): List<Stone> {
        val captured = mutableListOf<Stone>()
        var point = start
        while (point != null) {
            val occupant = arrangement.stoneAt(point) ?: break
            if (occupant.color == stone.color) break
            captured += occupant
            point = board.next(point, direction)
        }
        return captured
    }

    override fun apply(arrangement: Arrangement, board: Board): List<Stone> {
        val captured = captures(arrangement, board)
        require(captured.isNotEmpty()) { "Capture must remove at least one stone" }
        arrangement.move(stone, target)
        arrangement.remove(captured)
        return captured
    }
}

class Approach(stone: Stone, target: Point) : Capture(stone, target) {
    override fun capturedStones(
        from: Point,
        direction: Direction,
        arrangement: Arrangement,
        board: Board,
    ): List<Stone> = contiguousEnemies(board.next(target, direction), direction, arrangement, board)
}

class Withdrawal(stone: Stone, target: Point) : Capture(stone, target) {
    override fun capturedStones(
        from: Point,
        direction: Direction,
        arrangement: Arrangement,
        board: Board,
    ): List<Stone> = contiguousEnemies(
        board.next(from, direction.opposite()),
        direction.opposite(),
        arrangement,
        board,
    )
}
