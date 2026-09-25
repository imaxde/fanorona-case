package fanorona.ui

import fanorona.services.IGameService

class StartScreen(private val games: IGameService) {
    fun start(arguments: List<String>) {
        require(arguments.size == 2) { "Использование: start <белые> <чёрные>" }
        games.startGame(arguments[0], arguments[1])
    }
}
