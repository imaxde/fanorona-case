package fanorona.services

import fanorona.domain.Game
import fanorona.domain.Player
import fanorona.domain.PlayerStatistics
import fanorona.repositories.GameRepository
import fanorona.repositories.PlayerRepository

class StatisticsService(
    private val players: PlayerRepository,
    private val games: GameRepository,
) {
    fun players(): List<Player> = players.findAll()

    fun findPlayer(name: String): Player? = players.findByName(name)

    fun gamesOf(player: Player): List<Game> = games.findByPlayer(canonical(player))

    fun statisticsFor(player: Player): PlayerStatistics = games.statisticsFor(canonical(player))

    private fun canonical(player: Player): Player = players.findByName(player.name) ?: player
}
