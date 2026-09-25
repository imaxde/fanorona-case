package fanorona.domain

class Move(val side: Color) {
    private val recordedActions = mutableListOf<Action>()
    private val visited = mutableListOf<Point>()
    private var captured = 0
    private var direction: Direction? = null
    private var movingStone: Stone? = null

    val actions: List<Action>
        get() = recordedActions.toList()

    fun capturedCount(): Int = captured

    fun visitedPoints(): List<Point> = visited.toList()

    fun lastDirection(): Direction? = direction

    internal fun add(action: Action, origin: Point, direction: Direction, capturedCount: Int) {
        require(action.stone.color == side) { "Action belongs to the other side" }
        require(capturedCount >= 0)
        if (recordedActions.isEmpty()) {
            movingStone = action.stone
            visited += origin
        } else {
            require(action.stone === movingStone) { "A capture series must use one stone" }
        }
        recordedActions += action
        visited += action.target
        captured += capturedCount
        this.direction = direction
    }
}
