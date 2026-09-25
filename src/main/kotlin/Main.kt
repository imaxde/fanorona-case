package fanorona

import fanorona.repositories.InMemoryGameRepository
import fanorona.repositories.InMemoryPlayerRepository
import fanorona.services.ActionService
import fanorona.services.GameService
import fanorona.services.PlayerRegistryService
import fanorona.services.ReplayService
import fanorona.services.StatisticsService
import fanorona.setup.ClassicFanoronaFactory
import fanorona.setup.GameSetupFactory
import fanorona.ui.ConsoleApplication
import fanorona.ui.DesktopController
import fanorona.ui.SwingApplication
import java.io.InputStream
import java.io.PrintStream
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main(args: Array<String>) {
    when {
        args.isEmpty() || args.contentEquals(arrayOf("--gui")) -> runGui(ClassicFanoronaFactory())
        args.contentEquals(arrayOf("--console")) -> runConsole(ClassicFanoronaFactory(), System.`in`, System.out)
        args.contentEquals(arrayOf("--help")) -> {
            println("Запуск: ./gradlew run [--args=--gui|--args=--console]")
        }
        else -> throw IllegalArgumentException("Неизвестные параметры. Используйте --help")
    }
}

fun runGui(factory: GameSetupFactory) {
    val players = InMemoryPlayerRepository()
    val games = InMemoryGameRepository()
    val gameService = GameService(
        players,
        games,
        ActionService(factory.createBoard(), factory.createActionRules()),
        factory,
    )
    val controller = DesktopController(
        gameService,
        PlayerRegistryService(players),
        StatisticsService(players, games),
        ReplayService(games, factory),
        factory,
    )
    SwingUtilities.invokeLater {
        UIManager.getInstalledLookAndFeels().firstOrNull { it.name == "Nimbus" }?.let {
            UIManager.setLookAndFeel(it.className)
        }
        SwingApplication(controller).isVisible = true
    }
}

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
