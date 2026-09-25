package fanorona.domain

import fanorona.rules.DrawRule
import java.time.LocalDate

class Game(
    val whitePlayer: Player,
    val blackPlayer: Player,
    val board: Board,
    initialArrangement: Arrangement,
    private val drawRules: List<DrawRule> = emptyList(),
    val date: LocalDate = LocalDate.now(),
) {
    init {
        require(whitePlayer != blackPlayer) { "A player cannot play both sides" }
    }

    var id: Long = 0
        internal set

    var outcome: Outcome? = null
        private set

    var currentSide: Color = Color.WHITE
        private set

    private val initialPosition = initialArrangement.copy()
    private var currentArrangement = initialArrangement.copy()
    private val completedMoves = mutableListOf<Move>()
    private var pendingMove: Move? = null
    private val occurrences = mutableMapOf(
        (currentArrangement.occupiedPoints() to currentSide) to 1,
    )

    val arrangement: Arrangement
        get() = currentArrangement.copy()

    val initialArrangement: Arrangement
        get() = initialPosition.copy()

    val moves: List<Move>
        get() = completedMoves.toList()

    fun currentMove(): Move? = pendingMove

    fun startNextMove() {
        check(outcome == null) { "The game has ended" }
        check(pendingMove == null) { "A move is already in progress" }
        pendingMove = Move(currentSide)
    }

    fun apply(action: Action): Int {
        check(outcome == null) { "The game has ended" }
        require(action.stone.color == currentSide) { "It is the other side's turn" }
        val origin = requireNotNull(currentArrangement.pointOf(action.stone)) { "Stone is not on the board" }
        val direction = requireNotNull(action.direction(currentArrangement)) { "Action has no direction" }
        val nextArrangement = currentArrangement.copy()
        val captured = action.apply(nextArrangement, board)
        val move = pendingMove ?: Move(currentSide)
        move.add(action, origin, direction, captured.size)
        currentArrangement = nextArrangement
        pendingMove = move
        return captured.size
    }

    fun finishMove() {
        val move = checkNotNull(pendingMove) { "No move is in progress" }
        check(move.actions.isNotEmpty()) { "A move must contain an action" }
        completedMoves += move
        pendingMove = null
        currentSide = currentSide.opposite()
        val key = currentArrangement.occupiedPoints() to currentSide
        occurrences[key] = occurrences.getOrDefault(key, 0) + 1
        updateOutcome()
    }

    fun movesWithoutCapture(): Int =
        completedMoves.asReversed().takeWhile { it.capturedCount() == 0 }.size

    fun repetitionCount(): Int =
        occurrences.getOrDefault(currentArrangement.occupiedPoints() to currentSide, 0)

    fun updateOutcome() {
        check(pendingMove == null) { "Cannot determine the result during a move" }
        if (outcome != null) return
        outcome = when {
            currentArrangement.count(Color.WHITE) == 0 -> Outcome.BLACK_WIN
            currentArrangement.count(Color.BLACK) == 0 -> Outcome.WHITE_WIN
            drawRules.any { it.isDraw(this) } -> Outcome.DRAW
            else -> null
        }
    }
}
