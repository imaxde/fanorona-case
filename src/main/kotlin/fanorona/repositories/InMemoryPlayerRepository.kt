package fanorona.repositories

import fanorona.domain.Player
import java.util.Locale

class InMemoryPlayerRepository : PlayerRepository {
    private val players = linkedMapOf<String, Player>()

    override fun findAll(): List<Player> = players.values.toList()

    override fun findByName(name: String): Player? = players[key(name)]

    override fun save(player: Player) {
        val key = key(player.name)
        require(key !in players) { "Player name is already in use" }
        players[key] = player
    }

    private fun key(name: String): String = name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
}
