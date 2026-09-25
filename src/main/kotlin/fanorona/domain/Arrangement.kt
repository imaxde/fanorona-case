package fanorona.domain

class Arrangement(positions: Map<Point, Stone> = emptyMap()) {
    private val positions = positions.toMutableMap()

    init {
        require(this.positions.values.toSet().size == this.positions.size) {
            "The same stone cannot occupy multiple points"
        }
    }

    fun stoneAt(point: Point): Stone? = positions[point]

    fun pointOf(stone: Stone): Point? = positions.entries.firstOrNull { it.value === stone }?.key

    fun move(stone: Stone, to: Point) {
        val from = requireNotNull(pointOf(stone)) { "Stone is not on the board" }
        require(stoneAt(to) == null) { "Destination is occupied" }
        positions.remove(from)
        positions[to] = stone
    }

    fun remove(stones: List<Stone>) {
        require(stones.distinct().size == stones.size) { "A stone cannot be removed twice" }
        for (stone in stones) {
            val point = requireNotNull(pointOf(stone)) { "Stone is not on the board" }
            positions.remove(point)
        }
    }

    fun count(color: Color): Int = positions.values.count { it.color == color }

    fun copy(): Arrangement = Arrangement(positions)

    fun occupiedPoints(): Map<Point, Color> = positions.mapValues { it.value.color }
}
