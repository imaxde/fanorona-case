package fanorona.ui

import fanorona.domain.Board
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.border.EmptyBorder

class ReplayTab(onBack: () -> Unit, onNext: () -> Unit) : JPanel(BorderLayout(16, 14)) {
    private val board = BoardPanel()
    private val title = JLabel("Выберите завершённую партию во вкладке «Игроки»")
    private val step = JLabel(" ")
    private val back = JButton("Предыдущий ход").apply { name = "replayBackButton" }
    private val next = JButton("Следующий ход").apply { name = "replayNextButton" }

    init {
        background = DesktopTheme.background
        border = EmptyBorder(22, 22, 20, 22)
        title.font = DesktopTheme.title
        title.foreground = DesktopTheme.text
        step.font = DesktopTheme.body
        add(title, BorderLayout.NORTH)
        add(JScrollPane(board).apply { border = EmptyBorder(0, 0, 0, 0) }, BorderLayout.CENTER)
        add(JPanel(FlowLayout(FlowLayout.CENTER, 16, 6)).apply {
            isOpaque = false
            add(back)
            add(step)
            add(next)
        }, BorderLayout.SOUTH)
        back.addActionListener { onBack() }
        next.addActionListener { onNext() }
        render(null)
    }

    fun render(state: ReplayState?) {
        if (state == null) {
            title.text = "Выберите завершённую партию во вкладке «Игроки»"
            step.text = " "
            back.isEnabled = false
            next.isEnabled = false
            board.showBoard(Board(), emptyMap())
            return
        }
        val game = state.game
        title.text = "Повтор партии #${game.id} · ${game.whitePlayer.name} — ${game.blackPlayer.name}"
        step.text = "Ход ${state.step} из ${state.totalSteps}"
        back.isEnabled = state.step > 0
        next.isEnabled = state.step < state.totalSteps
        board.showBoard(game.board, state.stones)
    }
}
