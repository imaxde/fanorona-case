package fanorona.persistence

import fanorona.domain.Player
import fanorona.repositories.PlayerRepository
import java.util.Locale

class SqlitePlayerRepository(private val database: SqliteDatabase) : PlayerRepository {
    override fun findAll(): List<Player> = database.connection.prepareStatement(
        "SELECT name FROM players ORDER BY id",
    ).use { statement ->
        statement.executeQuery().use { rows ->
            buildList { while (rows.next()) add(Player(rows.getString("name"))) }
        }
    }

    override fun findByName(name: String): Player? = database.connection.prepareStatement(
        "SELECT name FROM players WHERE lookup_name = ?",
    ).use { statement ->
        statement.setString(1, key(name))
        statement.executeQuery().use { rows ->
            if (rows.next()) Player(rows.getString("name")) else null
        }
    }

    override fun save(player: Player) {
        require(findByName(player.name) == null) { "Player name is already in use" }
        database.transaction { connection ->
            connection.prepareStatement("INSERT INTO players(name, lookup_name) VALUES (?, ?)").use {
                it.setString(1, player.name)
                it.setString(2, key(player.name))
                it.executeUpdate()
            }
            connection.createStatement().use {
                it.executeUpdate("INSERT INTO player_stats(player_id) VALUES (last_insert_rowid())")
            }
        }
    }

    internal companion object {
        fun key(name: String): String = name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
    }
}
