package fanorona.ui

import fanorona.domain.Point
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.border.EmptyBorder

class GameTab(
    onBoardClick: (Point) -> Unit,
    onStart: (String, String) -> Unit,
    onEndMove: () -> Unit,
    onCancel: () -> Unit,
    onOpenPlayers: () -> Unit,
) : JPanel(BorderLayout(16, 14)) {
    private val board = BoardPanel(onBoardClick).apply { name = "gameBoard" }
    private val whitePlayer = JComboBox<String>().apply { name = "whitePlayerCombo" }
    private val blackPlayer = JComboBox<String>().apply { name = "blackPlayerCombo" }
    private val start = JButton("Начать партию").apply { name = "startGameButton" }
    private val endMove = JButton("Завершить ход").apply { name = "endMoveButton" }
    private val cancel = JButton("Отменить партию").apply { name = "cancelGameButton" }
    private val turn = JLabel()
    private val whiteCount = JLabel()
    private val blackCount = JLabel()
    private val moveDetails = JLabel()
    private val noCapture = JLabel()
    private val notice = JLabel(" ")

    init {
        background = DesktopTheme.background
        border = EmptyBorder(22, 22, 20, 22)

        val heading = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(JLabel("Фанорона").apply {
                font = DesktopTheme.title
                foreground = DesktopTheme.text
            }, BorderLayout.WEST)
            add(JLabel("Классическая доска 9 × 5").apply {
                font = DesktopTheme.body
                foreground = DesktopTheme.muted
            }, BorderLayout.EAST)
        }
        add(heading, BorderLayout.NORTH)
        add(JScrollPane(board).apply {
            border = EmptyBorder(0, 0, 0, 0)
            viewport.background = DesktopTheme.background
        }, BorderLayout.CENTER)

        val side = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = DesktopTheme.surface
            border = EmptyBorder(22, 20, 22, 20)
            preferredSize = Dimension(290, 0)
        }
        side.add(JLabel("Новая партия").apply { font = DesktopTheme.heading })
        side.add(Box.createVerticalStrut(18))
        side.add(JLabel("Белые").apply { font = DesktopTheme.body })
        side.add(Box.createVerticalStrut(5))
        whitePlayer.maximumSize = Dimension(Int.MAX_VALUE, 35)
        side.add(whitePlayer)
        side.add(Box.createVerticalStrut(12))
        side.add(JLabel("Чёрные").apply { font = DesktopTheme.body })
        side.add(Box.createVerticalStrut(5))
        blackPlayer.maximumSize = Dimension(Int.MAX_VALUE, 35)
        side.add(blackPlayer)
        side.add(Box.createVerticalStrut(14))
        start.maximumSize = Dimension(Int.MAX_VALUE, 38)
        side.add(start)
        side.add(Box.createVerticalStrut(8))
        side.add(JButton("Открыть реестр игроков").apply {
            name = "openPlayersButton"
            maximumSize = Dimension(Int.MAX_VALUE, 35)
            addActionListener { onOpenPlayers() }
        })
        side.add(Box.createVerticalStrut(30))
        side.add(JLabel("Состояние партии").apply { font = DesktopTheme.heading })
        side.add(Box.createVerticalStrut(14))
        turn.foreground = DesktopTheme.accent
        for (label in listOf(turn, whiteCount, blackCount, moveDetails, noCapture)) {
            label.font = DesktopTheme.body
            label.alignmentX = LEFT_ALIGNMENT
            side.add(label)
            side.add(Box.createVerticalStrut(9))
        }
        side.add(Box.createVerticalGlue())
        endMove.maximumSize = Dimension(Int.MAX_VALUE, 37)
        cancel.maximumSize = Dimension(Int.MAX_VALUE, 37)
        side.add(endMove)
        side.add(Box.createVerticalStrut(8))
        side.add(cancel)
        add(side, BorderLayout.EAST)

        val bottom = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            add(notice)
        }
        notice.font = DesktopTheme.body
        notice.foreground = DesktopTheme.danger
        add(bottom, BorderLayout.SOUTH)

        start.addActionListener {
            val white = whitePlayer.selectedItem as? String ?: return@addActionListener
            val black = blackPlayer.selectedItem as? String ?: return@addActionListener
            onStart(white, black)
        }
        endMove.addActionListener { onEndMove() }
        cancel.addActionListener { onCancel() }
    }

    fun render(state: BoardState, players: List<String>) {
        updatePlayers(whitePlayer, players)
        updatePlayers(blackPlayer, players)
        start.isEnabled = !state.active && players.size >= 2
        whitePlayer.isEnabled = !state.active
        blackPlayer.isEnabled = !state.active
        endMove.isEnabled = state.canEndMove
        cancel.isEnabled = state.active
        board.showBoard(state.board, state.stones, state.selected, state.movable, state.targets)

        turn.text = when {
            state.active -> "Ходят ${GameText.side(checkNotNull(state.currentSide))}: " +
                if (state.currentSide == fanorona.domain.Color.WHITE) state.whiteName else state.blackName
            state.outcome != null -> GameText.outcome(state.outcome)
            else -> "Выберите двух игроков"
        }
        whiteCount.text = if (state.whiteName == null) "Белые: ${state.whiteCount} камней"
        else "Белые · ${state.whiteName}: ${state.whiteCount}"
        blackCount.text = if (state.blackName == null) "Чёрные: ${state.blackCount} камней"
        else "Чёрные · ${state.blackName}: ${state.blackCount}"
        whiteCount.toolTipText = whiteCount.text
        blackCount.toolTipText = blackCount.text
        moveDetails.text = if (state.active) {
            "Снято за ход: ${state.capturedThisMove}"
        } else {
            "Выберите камень и точку"
        }
        noCapture.text = if (state.active) "Ходов без захвата: ${state.movesWithoutCapture}" else " "
    }

    fun showNotice(message: String) {
        notice.text = message.ifBlank { " " }
    }

    private fun updatePlayers(combo: JComboBox<String>, players: List<String>) {
        val selected = combo.selectedItem as? String
        if ((0 until combo.itemCount).map(combo::getItemAt) == players) return
        combo.model = DefaultComboBoxModel(players.toTypedArray())
        if (selected in players) combo.selectedItem = selected
    }
}
