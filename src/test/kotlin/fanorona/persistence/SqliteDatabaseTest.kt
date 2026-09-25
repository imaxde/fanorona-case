package fanorona.persistence

import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SqliteDatabaseTest {
    @Test
    fun `new database creates relational schema and enables foreign keys`() {
        val file = Files.createTempDirectory("fanorona-db-test").resolve("nested/game.db")
        SqliteDatabase(file).use { database ->
            database.connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA foreign_keys").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(1, rows.getInt(1))
                }
                statement.executeQuery("PRAGMA user_version").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(1, rows.getInt(1))
                }
                statement.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'").use { rows ->
                    val tables = buildSet { while (rows.next()) add(rows.getString(1)) }
                    assertTrue(tables.containsAll(setOf("players", "player_stats", "games", "moves", "actions")))
                }
            }
        }
        assertTrue(Files.exists(file))
        SqliteDatabase(file).use { database ->
            database.connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA user_version").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(1, rows.getInt(1))
                }
            }
        }
    }

    @Test
    fun `newer schema is rejected instead of modified`() {
        val file = Files.createTempFile("fanorona-future-db", ".db")
        DriverManager.getConnection("jdbc:sqlite:$file").use { connection ->
            connection.createStatement().use { it.execute("PRAGMA user_version = 2") }
        }
        assertFailsWith<IllegalStateException> { SqliteDatabase(file) }
    }

    @Test
    fun `failed transaction rolls back writes`() {
        val file = Files.createTempFile("fanorona-rollback", ".db")
        SqliteDatabase(file).use { database ->
            assertFailsWith<IllegalStateException> {
                database.transaction { connection ->
                    connection.prepareStatement("INSERT INTO players(name, lookup_name) VALUES (?, ?)").use {
                        it.setString(1, "Анна")
                        it.setString(2, "анна")
                        it.executeUpdate()
                    }
                    error("Сбой")
                }
            }
            database.connection.createStatement().use { statement ->
                statement.executeQuery("SELECT count(*) FROM players").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(0, rows.getInt(1))
                }
            }
        }
    }
}
