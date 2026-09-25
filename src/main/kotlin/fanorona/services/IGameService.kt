package fanorona.services

import fanorona.domain.Action
import fanorona.domain.Game
import fanorona.rules.Violation

fun interface GameListener {
    fun onGameChanged(game: Game)
}

interface IGameService {
    fun startGame(whiteName: String, blackName: String): Game

    fun perform(action: Action): List<Violation>

    fun endMove()

    fun cancelGame()

    fun availableActions(): List<Action>

    fun currentGame(): Game?

    fun addListener(listener: GameListener)
}
