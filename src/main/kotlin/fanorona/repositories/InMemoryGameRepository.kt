package fanorona.repositories

import fanorona.domain.Game
import fanorona.domain.Move
import fanorona.domain.Player
import fanorona.domain.PlayerStatistics
import fanorona.domain.Outcome

class InMemoryGameRepository : GameRepository {
    private val games = linkedMapOf<Long, Game>()
    private var nextId = 1L

    override fun save(game: Game) {
        require(game.outcome != null) { "Only completed games may be archived" }
        if (game.id == 0L) {
            game.id = nextId++
        }
        val existing = games[game.id]
        require(existing == null || existing === game) { "Game identifier is already in use" }
        games[game.id] = game
    }

    override fun findById(id: Long): Game? = games[id]

    override fun findByPlayer(player: Player): List<Game> = games.values.filter {
        it.whitePlayer == player || it.blackPlayer == player
    }

    override fun findMoves(gameId: Long): List<Move> = games[gameId]?.moves ?: emptyList()

    override fun statisticsFor(player: Player): PlayerStatistics {
        val played = findByPlayer(player)
        val wins = played.count {
            (it.whitePlayer == player && it.outcome == Outcome.WHITE_WIN) ||
                (it.blackPlayer == player && it.outcome == Outcome.BLACK_WIN)
        }
        val losses = played.count {
            (it.whitePlayer == player && it.outcome == Outcome.BLACK_WIN) ||
                (it.blackPlayer == player && it.outcome == Outcome.WHITE_WIN)
        }
        return PlayerStatistics(wins, losses, played.size)
    }
}
