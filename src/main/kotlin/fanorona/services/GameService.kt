package fanorona.services

import fanorona.domain.Action
import fanorona.domain.Capture
import fanorona.domain.Game
import fanorona.domain.Paika
import fanorona.domain.Player
import fanorona.repositories.GameRepository
import fanorona.repositories.PlayerRepository
import fanorona.rules.Violation
import fanorona.setup.GameSetupFactory
import java.time.Clock
import java.time.LocalDate

class GameService(
    private val players: PlayerRepository,
    private val games: GameRepository,
    private val actions: ActionService,
    private val factory: GameSetupFactory,
    private val clock: Clock = Clock.systemDefaultZone(),
) : IGameService {
    private var activeGame: Game? = null
    private val listeners = mutableListOf<GameListener>()

    override fun startGame(whiteName: String, blackName: String): Game {
        check(activeGame == null) { "A game is already active" }
        val white = normalizedName(whiteName)
        val black = normalizedName(blackName)
        require(white.isNotEmpty() && black.isNotEmpty()) { "Both player names are required" }
        require(!white.equals(black, ignoreCase = true)) { "Players must have distinct names" }

        val whitePlayer = players.findByName(white) ?: Player(white)
        val blackPlayer = players.findByName(black) ?: Player(black)
        val board = factory.createBoard()
        val game = Game(
            whitePlayer,
            blackPlayer,
            board,
            factory.createInitialArrangement(board),
            factory.createDrawRules(),
            LocalDate.now(clock),
        )
        if (players.findByName(white) == null) players.save(whitePlayer)
        if (players.findByName(black) == null) players.save(blackPlayer)
        activeGame = game
        notifyListeners(game)
        return game
    }

    override fun perform(action: Action): List<Violation> {
        val game = activeGame ?: return listOf(Violation("Нет активной партии"))
        val violations = actions.validate(action, game)
        if (violations.isNotEmpty()) return violations
        game.apply(action)
        if (action is Paika || game.arrangement.count(action.stone.color.opposite()) == 0 ||
            !actions.canContinueMove(game)
        ) {
            finishMove(game)
        } else {
            notifyListeners(game)
        }
        return emptyList()
    }

    override fun endMove() {
        val game = checkNotNull(activeGame) { "No active game" }
        check(game.currentMove()?.actions?.lastOrNull() is Capture) { "No capture series is in progress" }
        finishMove(game)
    }

    override fun cancelGame() {
        checkNotNull(activeGame) { "No active game" }
        activeGame = null
    }

    override fun availableActions(): List<Action> =
        activeGame?.let(actions::availableActions) ?: emptyList()

    override fun currentGame(): Game? = activeGame

    override fun addListener(listener: GameListener) {
        listeners += listener
    }

    private fun finishMove(game: Game) {
        game.finishMove()
        if (game.outcome != null) {
            games.save(game)
            activeGame = null
        }
        notifyListeners(game)
    }

    private fun notifyListeners(game: Game) {
        listeners.forEach { it.onGameChanged(game) }
    }

    private fun normalizedName(name: String): String =
        name.trim().replace(Regex("\\s+"), " ")
}
