package fanorona.services

import fanorona.domain.Player
import fanorona.repositories.PlayerRepository

class PlayerRegistryService(private val players: PlayerRepository) {
    fun register(name: String): Player {
        val normalized = name.trim().replace(Regex("\\s+"), " ")
        require(normalized.isNotEmpty()) { "Введите имя игрока" }
        require(players.findByName(normalized) == null) { "Игрок с таким именем уже зарегистрирован" }
        return Player(normalized).also(players::save)
    }

    fun players(): List<Player> = players.findAll()
}
