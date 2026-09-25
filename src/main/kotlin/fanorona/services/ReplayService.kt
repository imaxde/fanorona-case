package fanorona.services

import fanorona.domain.Arrangement
import fanorona.repositories.GameRepository
import fanorona.setup.GameSetupFactory

class ReplayService(
    private val games: GameRepository,
    private val factory: GameSetupFactory,
) {
    private var snapshots: List<Arrangement>? = null
    private var index = 0

    fun load(gameId: Long) {
        val game = requireNotNull(games.findById(gameId)) { "Game not found: $gameId" }
        val board = factory.createBoard()
        require(board.width == game.board.width && board.height == game.board.height) {
            "Replay board does not match the saved game"
        }
        val position = game.initialArrangement.copy()
        val frames = mutableListOf(position.copy())
        for (move in games.findMoves(gameId)) {
            for (action in move.actions) action.apply(position, board)
            frames += position.copy()
        }
        snapshots = frames
        index = 0
    }

    fun stepForward(): Boolean {
        val frames = checkNotNull(snapshots) { "No replay is loaded" }
        if (index == frames.lastIndex) return false
        index++
        return true
    }

    fun stepBack(): Boolean {
        checkNotNull(snapshots) { "No replay is loaded" }
        if (index == 0) return false
        index--
        return true
    }

    fun currentArrangement(): Arrangement =
        checkNotNull(snapshots) { "No replay is loaded" }[index].copy()
}
