package fanorona.persistence

import fanorona.domain.Board
import fanorona.domain.Color
import fanorona.domain.Game
import fanorona.domain.Move
import fanorona.domain.Outcome
import fanorona.domain.Player
import fanorona.domain.PlayerStatistics
import fanorona.domain.Point
import fanorona.repositories.GameRepository
import java.sql.Connection
import java.time.LocalDate

/** Stores a complete game and both player records in one transaction. */
class SqliteGameRepository(private val database: SqliteDatabase) : GameRepository {
    override fun save(game: Game) {
        val outcome = requireNotNull(game.outcome) { "Only completed games may be archived" }
        if (game.id != 0L) {
            val saved = requireNotNull(findById(game.id)) { "Saved game is missing" }
            require(saved.whitePlayer == game.whitePlayer && saved.blackPlayer == game.blackPlayer &&
                saved.date == game.date && saved.outcome == outcome &&
                saved.board.width == game.board.width && saved.board.height == game.board.height &&
                saved.initialArrangement.occupiedPoints() == game.initialArrangement.occupiedPoints() &&
                GameArchiveCodec.encodeMoves(saved) == GameArchiveCodec.encodeMoves(game)) {
                "Game identifier is already in use"
            }
            return
        }
        val position = GameArchiveCodec.encodePosition(game.board, game.initialArrangement)
        val moves = GameArchiveCodec.encodeMoves(game)
        val id = database.transaction { connection ->
            val whiteId = requireNotNull(playerId(connection, game.whitePlayer)) { "White player is not registered" }
            val blackId = requireNotNull(playerId(connection, game.blackPlayer)) { "Black player is not registered" }
            connection.prepareStatement(
                "INSERT INTO games(played_on, white_player_id, black_player_id, outcome, " +
                    "board_width, board_height, initial_position) VALUES (?, ?, ?, ?, ?, ?, ?)",
            ).use { statement ->
                statement.setString(1, game.date.toString())
                statement.setLong(2, whiteId)
                statement.setLong(3, blackId)
                statement.setString(4, outcome.name)
                statement.setInt(5, game.board.width)
                statement.setInt(6, game.board.height)
                statement.setString(7, position)
                statement.executeUpdate()
            }
            val gameId = connection.createStatement().use { statement ->
                statement.executeQuery("SELECT last_insert_rowid()").use { rows ->
                    check(rows.next())
                    rows.getLong(1)
                }
            }
            connection.prepareStatement("INSERT INTO moves(game_id, move_index, side) VALUES (?, ?, ?)").use { moveSql ->
                connection.prepareStatement(
                    "INSERT INTO actions(game_id, move_index, action_index, kind, " +
                        "from_x, from_y, to_x, to_y) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                ).use { actionSql ->
                    moves.forEachIndexed { moveIndex, move ->
                        moveSql.setLong(1, gameId)
                        moveSql.setInt(2, moveIndex)
                        moveSql.setString(3, move.side.name)
                        moveSql.executeUpdate()
                        move.actions.forEachIndexed { actionIndex, action ->
                            actionSql.setLong(1, gameId)
                            actionSql.setInt(2, moveIndex)
                            actionSql.setInt(3, actionIndex)
                            actionSql.setString(4, action.kind.name)
                            actionSql.setInt(5, action.from.x)
                            actionSql.setInt(6, action.from.y)
                            actionSql.setInt(7, action.to.x)
                            actionSql.setInt(8, action.to.y)
                            actionSql.executeUpdate()
                        }
                    }
                }
            }
            updateStatistics(connection, whiteId, outcome == Outcome.WHITE_WIN, outcome == Outcome.BLACK_WIN)
            updateStatistics(connection, blackId, outcome == Outcome.BLACK_WIN, outcome == Outcome.WHITE_WIN)
            gameId
        }
        game.id = id
    }

    override fun findById(id: Long): Game? {
        val header = database.connection.prepareStatement(
            "SELECT g.played_on, g.outcome, g.board_width, g.board_height, g.initial_position, " +
                "w.name AS white_name, b.name AS black_name FROM games g " +
                "JOIN players w ON w.id = g.white_player_id " +
                "JOIN players b ON b.id = g.black_player_id WHERE g.id = ?",
        ).use { statement ->
            statement.setLong(1, id)
            statement.executeQuery().use { rows ->
                if (!rows.next()) null else GameHeader(
                    Player(rows.getString("white_name")), Player(rows.getString("black_name")),
                    LocalDate.parse(rows.getString("played_on")), Outcome.valueOf(rows.getString("outcome")),
                    Board(rows.getInt("board_width"), rows.getInt("board_height")),
                    rows.getString("initial_position"),
                )
            }
        } ?: return null
        val restored = GameArchiveCodec.restore(
            header.white, header.black, header.board,
            GameArchiveCodec.decodePosition(header.board, header.position),
            header.date, readMoves(id), header.outcome,
        )
        restored.id = id
        return restored
    }

    override fun findByPlayer(player: Player): List<Game> {
        val ids = database.connection.prepareStatement(
            "SELECT g.id FROM games g JOIN players w ON w.id = g.white_player_id " +
                "JOIN players b ON b.id = g.black_player_id " +
                "WHERE w.lookup_name = ? OR b.lookup_name = ? ORDER BY g.id",
        ).use { statement ->
            val key = SqlitePlayerRepository.key(player.name)
            statement.setString(1, key)
            statement.setString(2, key)
            statement.executeQuery().use { rows ->
                buildList { while (rows.next()) add(rows.getLong(1)) }
            }
        }
        return ids.mapNotNull(::findById)
    }

    override fun findMoves(gameId: Long): List<Move> = findById(gameId)?.moves ?: emptyList()

    override fun statisticsFor(player: Player): PlayerStatistics = database.connection.prepareStatement(
        "SELECT s.wins, s.losses, s.games FROM player_stats s " +
            "JOIN players p ON p.id = s.player_id WHERE p.lookup_name = ?",
    ).use { statement ->
        statement.setString(1, SqlitePlayerRepository.key(player.name))
        statement.executeQuery().use { rows ->
            if (rows.next()) PlayerStatistics(rows.getInt(1), rows.getInt(2), rows.getInt(3))
            else PlayerStatistics(0, 0, 0)
        }
    }

    private fun playerId(connection: Connection, player: Player): Long? = connection.prepareStatement(
        "SELECT id FROM players WHERE lookup_name = ?",
    ).use { statement ->
        statement.setString(1, SqlitePlayerRepository.key(player.name))
        statement.executeQuery().use { rows -> if (rows.next()) rows.getLong(1) else null }
    }

    private fun updateStatistics(connection: Connection, id: Long, won: Boolean, lost: Boolean) {
        connection.prepareStatement(
            "UPDATE player_stats SET games = games + 1, wins = wins + ?, losses = losses + ? " +
                "WHERE player_id = ?",
        ).use { statement ->
            statement.setInt(1, if (won) 1 else 0)
            statement.setInt(2, if (lost) 1 else 0)
            statement.setLong(3, id)
            check(statement.executeUpdate() == 1) { "Player statistics are missing" }
        }
    }

    private fun readMoves(gameId: Long): List<StoredMove> {
        val headers = database.connection.prepareStatement(
            "SELECT move_index, side FROM moves WHERE game_id = ? ORDER BY move_index",
        ).use { statement ->
            statement.setLong(1, gameId)
            statement.executeQuery().use { rows ->
                buildList { while (rows.next()) add(rows.getInt("move_index") to Color.valueOf(rows.getString("side"))) }
            }
        }
        val actions = readActions(gameId)
        return headers.map { (index, side) -> StoredMove(side, actions[index].orEmpty()) }
    }

    private fun readActions(gameId: Long): Map<Int, List<StoredAction>> =
        database.connection.prepareStatement(
            "SELECT move_index, kind, from_x, from_y, to_x, to_y FROM actions " +
                "WHERE game_id = ? ORDER BY move_index, action_index",
        ).use { statement ->
            statement.setLong(1, gameId)
            statement.executeQuery().use { rows ->
                val grouped = mutableMapOf<Int, MutableList<StoredAction>>()
                while (rows.next()) {
                    grouped.getOrPut(rows.getInt("move_index")) { mutableListOf() }.add(
                        StoredAction(
                            StoredActionKind.valueOf(rows.getString("kind")),
                            Point(rows.getInt("from_x"), rows.getInt("from_y")),
                            Point(rows.getInt("to_x"), rows.getInt("to_y")),
                        ),
                    )
                }
                grouped
            }
        }

    private data class GameHeader(
        val white: Player,
        val black: Player,
        val date: LocalDate,
        val outcome: Outcome,
        val board: Board,
        val position: String,
    )
}
