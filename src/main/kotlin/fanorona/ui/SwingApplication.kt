package fanorona.ui

import fanorona.domain.Game
import fanorona.domain.Point
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder

class SwingApplication(private val controller: DesktopController) : JFrame("Фанорона") {
    private val tabs = JTabbedPane().apply { name = "tabs" }
    private val status = JLabel(" ").apply {
        name = "statusMessage"
        font = DesktopTheme.body
        foreground = DesktopTheme.danger
        border = EmptyBorder(5, 18, 10, 18)
    }
    private val gameTab = GameTab(
        onBoardClick = ::handleBoardClick,
        onStart = { white, black -> runAction { controller.startGame(white, black) } },
        onEndMove = { runAction(controller::endMove) },
        onCancel = ::confirmCancel,
        onOpenPlayers = { tabs.selectedIndex = 1 },
    )
    private val playersTab = PlayersTab(
        onRegister = { name -> runAction { controller.registerPlayer(name) } },
        onOpenReplay = ::openReplay,
    )
    private val replayTab = ReplayTab(
        onBack = { runAction { controller.replayBack() } },
        onNext = { runAction { controller.replayNext() } },
    )

    init {
        check(SwingUtilities.isEventDispatchThread()) { "Swing window must be created on the event dispatch thread" }
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(1000, 650)
        size = Dimension(1280, 780)
        contentPane.layout = BorderLayout()
        tabs.addTab("Партия", gameTab)
        tabs.addTab("Игроки", playersTab)
        tabs.addTab("Повтор", replayTab)
        contentPane.add(tabs, BorderLayout.CENTER)
        contentPane.add(status, BorderLayout.SOUTH)
        controller.setChangeListener {
            if (SwingUtilities.isEventDispatchThread()) refresh()
            else SwingUtilities.invokeLater(::refresh)
        }
        refresh()
        setLocationRelativeTo(null)
    }

    private fun refresh() {
        gameTab.render(controller.boardState(), controller.players().map { it.name })
        playersTab.render(controller.playerSummaries())
        replayTab.render(controller.replayState())
    }

    private fun handleBoardClick(point: Point) {
        when (val result = controller.clickBoard(point)) {
            is BoardClickResult.ChooseAction -> chooseCapture(result)
            is BoardClickResult.Invalid -> showMessage(result.message)
            BoardClickResult.ActionApplied, BoardClickResult.SelectionChanged -> showMessage("")
        }
    }

    private fun chooseCapture(result: BoardClickResult.ChooseAction) {
        val labels = result.options.map {
            val kind = when (it.kind) {
                ActionKind.PAIKA -> "Пайка"
                ActionKind.APPROACH -> "Атака"
                ActionKind.WITHDRAWAL -> "Отступление"
            }
            "$kind · снимет ${it.captured}"
        }.toTypedArray()
        val choice = JOptionPane.showOptionDialog(
            this,
            "Оба вида захвата допустимы. Выберите действие:",
            "Вид захвата",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            labels,
            labels.firstOrNull(),
        )
        if (choice >= 0) handleBoardClickResult(controller.chooseAction(result.options[choice].kind))
    }

    private fun handleBoardClickResult(result: BoardClickResult) {
        if (result is BoardClickResult.Invalid) showMessage(result.message) else showMessage("")
    }

    private fun confirmCancel() {
        val choice = JOptionPane.showConfirmDialog(
            this,
            "Отменить текущую партию? Незавершённая партия не попадёт в историю.",
            "Отмена партии",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
        )
        if (choice == JOptionPane.YES_OPTION) runAction(controller::cancelGame)
    }

    private fun openReplay(game: Game) {
        if (runAction { controller.loadReplay(game) }) tabs.selectedIndex = 2
    }

    private fun runAction(action: () -> Unit): Boolean = try {
        action()
        showMessage("")
        true
    } catch (exception: IllegalArgumentException) {
        showMessage(exception.message ?: "Некорректное действие")
        false
    } catch (exception: IllegalStateException) {
        showMessage(exception.message ?: "Действие сейчас недоступно")
        false
    }

    private fun showMessage(message: String) {
        status.text = message.ifBlank { " " }
        gameTab.showNotice(message)
    }
}
