package fanorona

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabasePathTest {
    @Test
    fun `database path follows XDG and falls back to user data directory`() {
        val home = Path.of("/tmp/fanorona-home")
        assertEquals(Path.of("/tmp/data/fanorona-case/games.db"),
            defaultDatabasePath(mapOf("XDG_DATA_HOME" to "/tmp/data"), home))
        assertEquals(home.resolve(".local/share/fanorona-case/games.db"),
            defaultDatabasePath(mapOf("XDG_DATA_HOME" to "relative/path"), home))
        assertEquals(home.resolve(".local/share/fanorona-case/games.db"),
            defaultDatabasePath(emptyMap(), home))
        assertEquals("games.db", defaultDatabasePath().fileName.toString())
    }
}
