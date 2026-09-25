package fanorona.persistence

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

/** Owns one SQLite connection shared by the repositories of an application session. */
class SqliteDatabase(path: Path) : AutoCloseable {
    val connection: Connection

    init {
        path.toAbsolutePath().parent?.let(Files::createDirectories)
        connection = DriverManager.getConnection("jdbc:sqlite:${path.toAbsolutePath()}")
        try {
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON")
                statement.execute("PRAGMA busy_timeout = 5000")
            }
            when (schemaVersion()) {
                0 -> transaction { database ->
                    val schema = checkNotNull(javaClass.getResourceAsStream("/db/schema.sql")) {
                        "Database schema is missing"
                    }.bufferedReader().use { it.readText() }
                    database.createStatement().use { statement ->
                        schema.split(';').map(String::trim).filter(String::isNotEmpty).forEach(statement::execute)
                        statement.execute("PRAGMA user_version = 1")
                    }
                }
                1 -> Unit
                else -> error("Unsupported database schema version")
            }
        } catch (failure: Throwable) {
            connection.close()
            throw failure
        }
    }

    fun <T> transaction(block: (Connection) -> T): T {
        check(connection.autoCommit) { "A transaction is already active" }
        connection.autoCommit = false
        try {
            val result = block(connection)
            connection.commit()
            return result
        } catch (failure: Throwable) {
            connection.rollback()
            throw failure
        } finally {
            connection.autoCommit = true
        }
    }

    override fun close() = connection.close()

    private fun schemaVersion(): Int = connection.createStatement().use { statement ->
        statement.executeQuery("PRAGMA user_version").use { rows ->
            check(rows.next())
            rows.getInt(1)
        }
    }
}
