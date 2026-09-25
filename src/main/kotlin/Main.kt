package fanorona

import fanorona.repositories.InMemoryGameRepository
import fanorona.repositories.InMemoryPlayerRepository
import fanorona.services.ActionService
import fanorona.services.GameService
import fanorona.services.ReplayService
import fanorona.services.StatisticsService
import fanorona.setup.ClassicFanoronaFactory
import fanorona.setup.GameSetupFactory
import fanorona.ui.ConsoleApplication
import java.io.InputStream
import java.io.PrintStream

fun main() = runConsole(ClassicFanoronaFactory(), System.`in`, System.out)

fun runConsole(
    factory: GameSetupFactory,
    input: InputStream,
    output: PrintStream,
) {
    val players = InMemoryPlayerRepository()
    val games = InMemoryGameRepository()
    val gameService = GameService(
        players,
        games,
        ActionService(factory.createBoard(), factory.createActionRules()),
        factory,
    )
    ConsoleApplication(
        gameService,
        StatisticsService(players, games),
        ReplayService(games, factory),
        factory,
        input,
        output,
    ).run()
}
