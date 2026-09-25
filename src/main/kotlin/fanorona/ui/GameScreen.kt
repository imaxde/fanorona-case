package fanorona.ui

import fanorona.domain.Action
import fanorona.domain.Approach
import fanorona.domain.Capture
import fanorona.domain.Color
import fanorona.domain.Game
import fanorona.domain.Paika
import fanorona.domain.Point
import fanorona.domain.Withdrawal
import fanorona.services.GameListener
import fanorona.services.IGameService
import java.io.PrintStream
import java.util.Locale

class GameScreen(
    private val games: IGameService,
    private val output: PrintStream,
) : GameListener {
    init {
        games.addListener(this)
    }

    override fun onGameChanged(game: Game) {
        val result = game.outcome
        if (result == null) {
            output.println("Ход: ${GameText.side(game.currentSide)}")
        } else {
            output.println("Партия #${game.id}: ${GameText.outcome(result)}")
        }
        output.println(BoardRenderer.render(game.arrangement, game.board))
    }

    fun board() {
        val game = requireGame()
        output.println(BoardRenderer.render(game.arrangement, game.board))
    }

    fun status() {
        val game = requireGame()
        output.println("Ход: ${GameText.side(game.currentSide)}")
        output.println("Белые: ${game.arrangement.count(Color.WHITE)}")
        output.println("Чёрные: ${game.arrangement.count(Color.BLACK)}")
        output.println("Ходов без захвата: ${game.movesWithoutCapture()}")
        game.currentMove()?.let { output.println("Снято в текущем ходе: ${it.capturedCount()}") }
    }

    fun actions() {
        val game = requireGame()
        val options = games.availableActions()
        if (options.isEmpty()) {
            output.println("Доступных действий нет")
            return
        }
        for ((index, action) in options.withIndex()) {
            val from = checkNotNull(game.arrangement.pointOf(action.stone))
            val type = when (action) {
                is Paika -> "paika"
                is Approach -> "approach"
                is Withdrawal -> "withdrawal"
            }
            val count = if (action is Capture) action.captures(game.arrangement, game.board).size else 0
            output.println("${index + 1}. $type ${format(from)} ${format(action.target)} (снимает: $count)")
        }
    }

    fun move(arguments: List<String>) {
        require(arguments.size == 3) { "Использование: move <paika|approach|withdrawal> <x,y> <x,y>" }
        val game = requireGame()
        val from = coordinate(arguments[1])
        val target = coordinate(arguments[2])
        val stone = requireNotNull(game.arrangement.stoneAt(from)) { "На исходной точке нет камня" }
        val action: Action = when (arguments[0].lowercase(Locale.ROOT)) {
            "paika", "пайка" -> Paika(stone, target)
            "approach", "атака" -> Approach(stone, target)
            "withdrawal", "отступление" -> Withdrawal(stone, target)
            else -> throw IllegalArgumentException("Неизвестный тип действия")
        }
        games.perform(action).forEach { output.println("Ошибка: ${it.message}") }
    }

    fun endMove() = games.endMove()

    fun cancel() {
        games.cancelGame()
        output.println("Партия отменена")
    }

    private fun requireGame(): Game = checkNotNull(games.currentGame()) { "Нет активной партии" }

    private fun coordinate(text: String): Point {
        val parts = text.split(',')
        require(parts.size == 2) { "Координата должна иметь вид x,y" }
        val x = parts[0].toIntOrNull()
        val y = parts[1].toIntOrNull()
        require(x != null && y != null) { "Координата должна иметь вид x,y" }
        return Point(x, y)
    }

    private fun format(point: Point): String = "${point.x},${point.y}"
}
