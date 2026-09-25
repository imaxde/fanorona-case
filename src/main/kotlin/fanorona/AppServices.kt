package fanorona

import fanorona.persistence.SqliteDatabase
import fanorona.persistence.SqliteGameRepository
import fanorona.persistence.SqlitePlayerRepository
import fanorona.services.ActionService
import fanorona.services.GameService
import fanorona.services.PlayerRegistryService
import fanorona.services.ReplayService
import fanorona.services.StatisticsService
import fanorona.setup.GameSetupFactory

/** Connects both user interfaces to the same persistent repositories. */
internal class AppServices(database: SqliteDatabase, factory: GameSetupFactory) {
    private val players = SqlitePlayerRepository(database)
    private val games = SqliteGameRepository(database)
    val gameService = GameService(
        players, games, ActionService(factory.createBoard(), factory.createActionRules()), factory,
    )
    val registry = PlayerRegistryService(players)
    val statistics = StatisticsService(players, games)
    val replay = ReplayService(games, factory)
}
