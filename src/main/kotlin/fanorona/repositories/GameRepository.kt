package fanorona.repositories

import fanorona.domain.Game
import fanorona.domain.Move
import fanorona.domain.Player
import fanorona.domain.PlayerStatistics

interface GameRepository {
    fun save(game: Game)

    fun findById(id: Long): Game?

    fun findByPlayer(player: Player): List<Game>

    fun findMoves(gameId: Long): List<Move>

    fun statisticsFor(player: Player): PlayerStatistics
}
