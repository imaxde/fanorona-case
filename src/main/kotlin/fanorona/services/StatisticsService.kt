package fanorona.services

import fanorona.domain.Game
import fanorona.domain.Outcome
import fanorona.domain.Player
import fanorona.repositories.GameRepository
import fanorona.repositories.PlayerRepository

class StatisticsService(
    private val players: PlayerRepository,
    private val games: GameRepository,
) {
    fun players(): List<Player> = players.findAll()

    fun findPlayer(name: String): Player? = players.findByName(name)

    fun gamesOf(player: Player): List<Game> = games.findByPlayer(canonical(player))

    fun statisticsFor(player: Player): PlayerStatistics {
        val actualPlayer = canonical(player)
        val played = gamesOf(actualPlayer)
        val wins = played.count {
            (it.whitePlayer == actualPlayer && it.outcome == Outcome.WHITE_WIN) ||
                (it.blackPlayer == actualPlayer && it.outcome == Outcome.BLACK_WIN)
        }
        val losses = played.count {
            (it.whitePlayer == actualPlayer && it.outcome == Outcome.BLACK_WIN) ||
                (it.blackPlayer == actualPlayer && it.outcome == Outcome.WHITE_WIN)
        }
        return PlayerStatistics(wins, losses, played.size)
    }

    private fun canonical(player: Player): Player = players.findByName(player.name) ?: player
}
