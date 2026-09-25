package fanorona.persistence

import fanorona.domain.Player
import fanorona.services.PlayerRegistryService
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SqlitePlayerRepositoryTest {
    @Test
    fun `registered players survive reopening and names remain unique`() {
        val file = Files.createTempFile("fanorona-players", ".db")
        SqliteDatabase(file).use { database ->
            val players = SqlitePlayerRepository(database)
            val registry = PlayerRegistryService(players)
            assertEquals(Player("Анна Ли"), registry.register("  Анна   Ли  "))
            assertEquals(Player("O'Brien; DROP TABLE players;"), registry.register("O'Brien; DROP TABLE players;"))
            assertFailsWith<IllegalArgumentException> { registry.register("анна ли") }
        }
        SqliteDatabase(file).use { database ->
            val players = SqlitePlayerRepository(database)
            assertEquals(Player("Анна Ли"), players.findByName(" АННА   ЛИ "))
            assertEquals(listOf("Анна Ли", "O'Brien; DROP TABLE players;"), players.findAll().map(Player::name))
            assertNull(players.findByName("Неизвестный"))
            assertFailsWith<IllegalArgumentException> { players.save(Player("анна ли")) }
        }
    }
}
