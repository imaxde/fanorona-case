package fanorona.services

import fanorona.domain.Action
import fanorona.domain.Color
import fanorona.domain.Game
import fanorona.rules.Violation

class InvariantCheckingGameService(inner: IGameService) : GameServiceDecorator(inner) {
    override fun startGame(whiteName: String, blackName: String): Game =
        super.startGame(whiteName, blackName).also(::checkInvariants)

    override fun perform(action: Action): List<Violation> =
        super.perform(action).also { currentGame()?.let(::checkInvariants) }

    override fun endMove() {
        super.endMove()
        currentGame()?.let(::checkInvariants)
    }

    private fun checkInvariants(game: Game) {
        check(inner.currentGame() === game)
        check(game.whitePlayer != game.blackPlayer)
        check(game.arrangement.count(Color.WHITE) <= game.initialArrangement.count(Color.WHITE))
        check(game.arrangement.count(Color.BLACK) <= game.initialArrangement.count(Color.BLACK))
        check(game.arrangement.occupiedPoints().keys.all {
            game.board.pointAt(it.x, it.y) != null
        })
        check(game.currentMove()?.side == null || game.currentMove()?.side == game.currentSide)
        check(game.currentMove()?.visitedPoints()?.distinct()?.size ==
            game.currentMove()?.visitedPoints()?.size)
    }
}
