package fanorona.ui

import fanorona.domain.Player
import fanorona.services.StatisticsService
import java.io.PrintStream

class StatisticsScreen(
    private val statistics: StatisticsService,
    private val output: PrintStream,
) {
    fun players() {
        val players = statistics.players()
        if (players.isEmpty()) output.println("Игроков пока нет")
        else players.forEach { output.println(it.name) }
    }

    fun statistics(arguments: List<String>) {
        val player = player(arguments, "stats")
        val result = statistics.statisticsFor(player)
        output.println("${player.name}: партий: ${result.games}, победы: ${result.wins}, " +
            "поражения: ${result.losses}, ничьи: ${result.draws}")
    }

    fun history(arguments: List<String>) {
        val player = player(arguments, "history")
        val games = statistics.gamesOf(player)
        if (games.isEmpty()) {
            output.println("Завершённых партий нет")
            return
        }
        games.forEach { game ->
            output.println("Партия #${game.id}: ${game.date}, " +
                "${GameText.outcome(checkNotNull(game.outcome))}, " +
                "белые: ${game.whitePlayer.name}, чёрные: ${game.blackPlayer.name}")
        }
    }

    private fun player(arguments: List<String>, command: String): Player {
        require(arguments.size == 1) { "Использование: $command <имя>" }
        return requireNotNull(statistics.findPlayer(arguments.single())) { "Игрок не найден" }
    }
}
