package fanorona.repositories

import fanorona.domain.Player

interface PlayerRepository {
    fun findAll(): List<Player>

    fun findByName(name: String): Player?

    fun save(player: Player)
}
