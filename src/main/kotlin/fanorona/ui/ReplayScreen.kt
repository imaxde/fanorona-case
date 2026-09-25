package fanorona.ui

import fanorona.services.ReplayService
import fanorona.setup.GameSetupFactory
import java.io.PrintStream

class ReplayScreen(
    private val replay: ReplayService,
    private val factory: GameSetupFactory,
    private val output: PrintStream,
) {
    private var loaded = false
    private var step = 0

    fun load(arguments: List<String>) {
        require(arguments.size == 1) { "Использование: replay <id>" }
        val id = arguments.single().toLongOrNull()
        require(id != null && id > 0) { "Номер партии должен быть положительным целым числом" }
        replay.load(id)
        loaded = true
        step = 0
        show("Повтор партии #$id")
    }

    fun next() {
        check(loaded) { "Сначала загрузите повтор" }
        if (replay.stepForward()) {
            step++
            show("Следующий ход")
        } else {
            output.println("Конец записи")
        }
    }

    fun back() {
        check(loaded) { "Сначала загрузите повтор" }
        if (replay.stepBack()) {
            step--
            show("Предыдущий ход")
        } else {
            output.println("Начало записи")
        }
    }

    private fun show(label: String) {
        output.println("$label. Шаг $step")
        output.println(BoardRenderer.render(replay.currentArrangement(), factory.createBoard()))
    }
}
