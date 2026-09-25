package fanorona.ui

import fanorona.services.IGameService
import fanorona.services.ReplayService
import fanorona.services.StatisticsService
import fanorona.setup.GameSetupFactory
import java.io.InputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.sql.SQLException

class ConsoleApplication(
    games: IGameService,
    statistics: StatisticsService,
    replay: ReplayService,
    factory: GameSetupFactory,
    input: InputStream,
    private val output: PrintStream,
) {
    private val reader = input.bufferedReader(StandardCharsets.UTF_8)
    private val startScreen = StartScreen(games)
    private val gameScreen = GameScreen(games, output)
    private val statisticsScreen = StatisticsScreen(statistics, output)
    private val replayScreen = ReplayScreen(replay, factory, output)

    fun run() {
        output.println("Фанорона. Введите help для списка команд.")
        while (true) {
            output.print("> ")
            output.flush()
            val line = reader.readLine() ?: break
            try {
                val tokens = CommandParser.parse(line)
                if (tokens.isEmpty()) continue
                val arguments = tokens.drop(1)
                when (tokens.first().lowercase()) {
                    "help" -> help()
                    "start" -> startScreen.start(arguments)
                    "board" -> gameScreen.board()
                    "status" -> gameScreen.status()
                    "actions" -> gameScreen.actions()
                    "move" -> gameScreen.move(arguments)
                    "end" -> gameScreen.endMove()
                    "cancel" -> gameScreen.cancel()
                    "players" -> statisticsScreen.players()
                    "stats" -> statisticsScreen.statistics(arguments)
                    "history" -> statisticsScreen.history(arguments)
                    "replay" -> replayScreen.load(arguments)
                    "next" -> replayScreen.next()
                    "back" -> replayScreen.back()
                    "quit", "exit" -> return
                    else -> output.println("Неизвестная команда. Введите help.")
                }
            } catch (exception: IllegalArgumentException) {
                output.println("Ошибка: ${exception.message}")
            } catch (exception: IllegalStateException) {
                output.println("Ошибка: ${exception.message}")
            } catch (exception: SQLException) {
                output.println("Ошибка базы данных: ${exception.message}")
            }
        }
    }

    private fun help() {
        output.println("start <белые> <чёрные> — новая партия; имена с пробелами берите в кавычки")
        output.println("players, board, status, actions")
        output.println("move <paika|approach|withdrawal> <x,y> <x,y>; end; cancel")
        output.println("stats <имя>; history <имя>; replay <id>; next; back; quit")
        output.println("Координаты: x от 0 до 8, y от 0 до 4. W — белые, B — чёрные.")
    }
}
