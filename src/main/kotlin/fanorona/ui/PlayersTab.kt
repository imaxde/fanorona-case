package fanorona.ui

import fanorona.domain.Game
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.border.EmptyBorder

class PlayersTab(
    onRegister: (String) -> Boolean,
    onOpenReplay: (Game) -> Unit,
) : JPanel(BorderLayout(18, 18)) {
    private val nameField = JTextField().apply { name = "playerNameField" }
    private val registerButton = JButton("Зарегистрировать").apply { name = "registerPlayerButton" }
    private val playersModel = DefaultListModel<PlayerSummary>()
    private val playerList = JList(playersModel).apply {
        name = "playerList"
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean,
            ): Component {
                val text = (value as? PlayerSummary)?.player?.name ?: ""
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus)
            }
        }
    }
    private val historyModel = DefaultListModel<Game>()
    private val historyList = JList(historyModel).apply {
        name = "historyList"
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean,
            ): Component {
                val game = value as? Game
                val label = if (game == null) "" else {
                    "#${game.id} · ${game.date} · ${GameText.outcome(checkNotNull(game.outcome))}"
                }
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus)
            }
        }
    }
    private val openReplay = JButton("Открыть повтор").apply { name = "openReplayButton" }
    private val statistics = JLabel("Выберите игрока")
    private val historyTitle = JLabel("История завершённых партий")

    init {
        background = DesktopTheme.background
        border = EmptyBorder(24, 24, 24, 24)

        val heading = JLabel("Игроки и статистика").apply {
            font = DesktopTheme.title
            foreground = DesktopTheme.text
        }
        add(heading, BorderLayout.NORTH)

        val left = JPanel(BorderLayout(0, 12)).apply {
            background = DesktopTheme.surface
            border = EmptyBorder(20, 20, 20, 20)
            minimumSize = Dimension(260, 200)
        }
        val registerPanel = JPanel(GridLayout(3, 1, 0, 7)).apply {
            isOpaque = false
            add(JLabel("Имя нового игрока").apply { font = DesktopTheme.heading })
            add(nameField)
            add(registerButton)
        }
        left.add(registerPanel, BorderLayout.NORTH)
        left.add(JScrollPane(playerList), BorderLayout.CENTER)

        val right = JPanel(BorderLayout(0, 15)).apply {
            background = DesktopTheme.surface
            border = EmptyBorder(20, 20, 20, 20)
        }
        val summary = JPanel(GridLayout(2, 1, 0, 8)).apply {
            isOpaque = false
            add(statistics)
            add(historyTitle)
        }
        statistics.font = DesktopTheme.body
        historyTitle.font = DesktopTheme.heading
        right.add(summary, BorderLayout.NORTH)
        right.add(JScrollPane(historyList), BorderLayout.CENTER)
        right.add(openReplay, BorderLayout.SOUTH)

        add(JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right).apply {
            resizeWeight = 0.35
            dividerLocation = 350
            border = EmptyBorder(0, 0, 0, 0)
        }, BorderLayout.CENTER)

        registerButton.addActionListener {
            if (onRegister(nameField.text)) nameField.text = ""
        }
        nameField.addActionListener { registerButton.doClick() }
        playerList.addListSelectionListener { if (!it.valueIsAdjusting) updateDetails() }
        historyList.addListSelectionListener { if (!it.valueIsAdjusting) updateReplayButton() }
        openReplay.addActionListener { historyList.selectedValue?.let(onOpenReplay) }
        updateReplayButton()
    }

    fun render(summaries: List<PlayerSummary>) {
        val selectedName = playerList.selectedValue?.player?.name
        playersModel.clear()
        summaries.forEach(playersModel::addElement)
        val index = summaries.indexOfFirst { it.player.name == selectedName }
        if (summaries.isNotEmpty()) playerList.selectedIndex = if (index >= 0) index else 0
        updateDetails()
    }

    private fun updateDetails() {
        val selectedGame = historyList.selectedValue?.id
        val summary = playerList.selectedValue
        historyModel.clear()
        if (summary == null) {
            statistics.text = "Зарегистрированных игроков пока нет"
        } else {
            val result = summary.statistics
            statistics.text = "${summary.player.name} · игр: ${result.games}, побед: ${result.wins}, " +
                "поражений: ${result.losses}, ничьих: ${result.draws}"
            summary.history.forEach(historyModel::addElement)
            val index = summary.history.indexOfFirst { it.id == selectedGame }
            if (index >= 0) historyList.selectedIndex = index
        }
        updateReplayButton()
    }

    private fun updateReplayButton() {
        openReplay.isEnabled = historyList.selectedValue != null
    }
}
