package fanorona.ui

import fanorona.domain.Action
import fanorona.domain.Approach
import fanorona.domain.Capture
import fanorona.domain.Color
import fanorona.domain.Game
import fanorona.domain.Paika
import fanorona.domain.Player
import fanorona.domain.Point
import fanorona.domain.Withdrawal
import fanorona.services.IGameService
import fanorona.services.PlayerRegistryService
import fanorona.domain.PlayerStatistics
import fanorona.services.ReplayService
import fanorona.services.StatisticsService
import fanorona.setup.GameSetupFactory

class DesktopController(
    private val games: IGameService,
    private val registry: PlayerRegistryService,
    private val statistics: StatisticsService,
    private val replay: ReplayService,
    private val factory: GameSetupFactory,
) {
    private var changeListener: () -> Unit = {}

    private var selected: Point? = null
    private var pendingChoices = emptyList<Action>()
    private var lastGame: Game? = null
    private var replayGame: Game? = null
    private var replayStep = 0

    init {
        games.addListener { game ->
            lastGame = game
            if (game.currentMove() == null) selected = null
            changeListener()
        }
    }

    fun setChangeListener(listener: () -> Unit) {
        changeListener = listener
    }

    fun registerPlayer(name: String): Player = registry.register(name).also { changeListener() }

    fun players(): List<Player> = registry.players()

    fun playerSummaries(): List<PlayerSummary> = players().map { player ->
        PlayerSummary(player, statistics.statisticsFor(player), statistics.gamesOf(player))
    }

    fun startGame(whiteName: String, blackName: String) {
        require(statistics.findPlayer(whiteName) != null) { "Зарегистрируйте игрока за белых" }
        require(statistics.findPlayer(blackName) != null) { "Зарегистрируйте игрока за чёрных" }
        require(!whiteName.equals(blackName, ignoreCase = true)) { "Выберите разных игроков" }
        selected = null
        pendingChoices = emptyList()
        games.startGame(whiteName, blackName)
    }

    fun clickBoard(point: Point): BoardClickResult {
        val game = games.currentGame() ?: return BoardClickResult.Invalid("Сначала начните партию")
        if (point == selected) {
            selected = null
            pendingChoices = emptyList()
            changeListener()
            return BoardClickResult.SelectionChanged
        }

        val available = games.availableActions()
        val arrangement = game.arrangement
        val stone = arrangement.stoneAt(point)
        if (stone != null && stone.color == game.currentSide && available.any { it.stone === stone }) {
            selected = point
            pendingChoices = emptyList()
            changeListener()
            return BoardClickResult.SelectionChanged
        }

        val origin = selected ?: return BoardClickResult.Invalid("Выберите камень с доступным ходом")
        val candidates = available.filter { arrangement.pointOf(it.stone) == origin && it.target == point }
        if (candidates.isEmpty()) return BoardClickResult.Invalid("Недоступное действие")
        if (candidates.size == 1) return perform(candidates.single())

        pendingChoices = candidates
        return BoardClickResult.ChooseAction(candidates.map { action ->
            ActionOption(
                when (action) {
                    is Paika -> ActionKind.PAIKA
                    is Approach -> ActionKind.APPROACH
                    is Withdrawal -> ActionKind.WITHDRAWAL
                },
                if (action is Capture) action.captures(arrangement, game.board).size else 0,
            )
        })
    }

    fun chooseAction(kind: ActionKind): BoardClickResult {
        val action = pendingChoices.firstOrNull {
            when (kind) {
                ActionKind.PAIKA -> it is Paika
                ActionKind.APPROACH -> it is Approach
                ActionKind.WITHDRAWAL -> it is Withdrawal
            }
        } ?: return BoardClickResult.Invalid("Выберите доступный вид действия")
        return perform(action)
    }

    fun endMove() = games.endMove()

    fun cancelGame() {
        games.cancelGame()
        selected = null
        pendingChoices = emptyList()
        lastGame = null
        changeListener()
    }

    fun boardState(): BoardState {
        val active = games.currentGame()
        val shown = active ?: lastGame
        val board = shown?.board ?: factory.createBoard()
        val arrangement = shown?.arrangement ?: factory.createInitialArrangement(board)
        val available = if (active == null) emptyList() else games.availableActions()
        val origins = available.mapNotNull { arrangement.pointOf(it.stone) }.toSet()
        val targets = available.filter { arrangement.pointOf(it.stone) == selected }.map { it.target }.toSet()
        return BoardState(
            board = board,
            stones = arrangement.occupiedPoints(),
            active = active != null,
            selected = selected,
            movable = origins,
            targets = targets,
            whiteName = shown?.whitePlayer?.name,
            blackName = shown?.blackPlayer?.name,
            currentSide = active?.currentSide,
            whiteCount = arrangement.count(Color.WHITE),
            blackCount = arrangement.count(Color.BLACK),
            capturedThisMove = active?.currentMove()?.capturedCount() ?: 0,
            movesWithoutCapture = shown?.movesWithoutCapture() ?: 0,
            canEndMove = active?.currentMove()?.actions?.lastOrNull() is Capture,
            outcome = shown?.outcome,
        )
    }

    fun statistics(name: String): PlayerStatistics = statistics.statisticsFor(player(name))

    fun history(name: String): List<Game> = statistics.gamesOf(player(name))

    fun loadReplay(game: Game) {
        replay.load(game.id)
        replayGame = game
        replayStep = 0
        changeListener()
    }

    fun replayNext(): Boolean {
        if (!replay.stepForward()) return false
        replayStep++
        changeListener()
        return true
    }

    fun replayBack(): Boolean {
        if (!replay.stepBack()) return false
        replayStep--
        changeListener()
        return true
    }

    fun replayState(): ReplayState? = replayGame?.let {
        ReplayState(it, replay.currentArrangement().occupiedPoints(), replayStep, it.moves.size)
    }

    private fun player(name: String): Player =
        requireNotNull(statistics.findPlayer(name)) { "Игрок не найден" }

    private fun perform(action: Action): BoardClickResult {
        val previous = selected
        selected = action.target
        pendingChoices = emptyList()
        val violations = games.perform(action)
        if (violations.isNotEmpty()) {
            selected = previous
            changeListener()
            return BoardClickResult.Invalid(violations.joinToString("; ") { it.message })
        }
        return BoardClickResult.ActionApplied
    }
}
