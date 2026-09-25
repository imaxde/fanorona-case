package fanorona.repositories

import fanorona.domain.Game
import fanorona.domain.Move
import fanorona.domain.Player

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
}
