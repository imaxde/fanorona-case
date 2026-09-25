package fanorona.services

import fanorona.domain.Action
import fanorona.domain.Game
import fanorona.rules.Violation

abstract class GameServiceDecorator(protected val inner: IGameService) : IGameService {
    override fun startGame(whiteName: String, blackName: String): Game =
        inner.startGame(whiteName, blackName)

    override fun perform(action: Action): List<Violation> = inner.perform(action)

    override fun endMove() = inner.endMove()

    override fun cancelGame() = inner.cancelGame()

    override fun availableActions(): List<Action> = inner.availableActions()

    override fun currentGame(): Game? = inner.currentGame()

    override fun addListener(listener: GameListener) = inner.addListener(listener)
}
