package fanorona.ui

import fanorona.domain.Game
import fanorona.domain.Color
import fanorona.domain.Point
import fanorona.repositories.InMemoryGameRepository
import fanorona.repositories.InMemoryPlayerRepository
import fanorona.services.ActionService
import fanorona.services.GameService
import fanorona.services.PlayerRegistryService
import fanorona.services.ReplayService
import fanorona.services.StatisticsService
import fanorona.setup.PositionFactory
import java.awt.Component
import java.awt.Container
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JList
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JTabbedPane
import javax.swing.JTextField
import javax.swing.SwingUtilities
import java.awt.Window
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwingApplicationTest {
    @Test
    fun `players can finish a game and replay it through Swing controls`() {
        val factory = PositionFactory("W.B......\n.........\n.........\n.........\n.........")
        val controller = controller(factory)
        lateinit var window: SwingApplication

        SwingUtilities.invokeAndWait {
            window = SwingApplication(controller)
            window.isVisible = true
        }
        try {
            SwingUtilities.invokeAndWait {
                val name = find<JTextField>(window, "playerNameField")
                val register = find<JButton>(window, "registerPlayerButton")
                name.text = "Анна"
                register.doClick()
                name.text = "Борис"
                register.doClick()

                find<JComboBox<*>>(window, "whitePlayerCombo").selectedItem = "Анна"
                find<JComboBox<*>>(window, "blackPlayerCombo").selectedItem = "Борис"
                find<JButton>(window, "startGameButton").doClick()
                assertTrue(controller.boardState().active)

                val board = find<BoardPanel>(window, "gameBoard")
                click(board, Point(0, 0))
                click(board, Point(1, 0))
                assertFalse(controller.boardState().active)
                assertEquals(1, controller.statistics("Анна").wins)

                find<JList<*>>(window, "playerList").selectedIndex = 0
                val history = find<JList<Game>>(window, "historyList")
                assertEquals(1, history.model.size)
                history.selectedIndex = 0
                find<JButton>(window, "openReplayButton").doClick()
                assertEquals(0, controller.replayState()?.step)
                find<JButton>(window, "replayNextButton").doClick()
                assertEquals(1, controller.replayState()?.step)
                find<JButton>(window, "replayBackButton").doClick()
                assertEquals(0, controller.replayState()?.step)
            }
        } finally {
            SwingUtilities.invokeAndWait { window.dispose() }
        }
    }

    @Test
    fun `registry errors capture series and cancellation work through Swing controls`() {
        val factory = PositionFactory("...B.....\n.........\n..W.B....\n.........\n.........")
        val controller = controller(factory)
        lateinit var window: SwingApplication
        SwingUtilities.invokeAndWait { window = SwingApplication(controller).apply { isVisible = true } }
        try {
            SwingUtilities.invokeAndWait {
                val name = find<JTextField>(window, "playerNameField")
                name.text = "Анна"
                name.postActionEvent()
                name.text = "Борис"
                find<JButton>(window, "registerPlayerButton").doClick()
                name.text = "АННА"
                find<JButton>(window, "registerPlayerButton").doClick()
                assertTrue(find<JLabel>(window, "statusMessage").text.contains("уже зарегистрирован"))

                find<JButton>(window, "openPlayersButton").doClick()
                assertEquals(1, find<JTabbedPane>(window, "tabs").selectedIndex)
                find<JTabbedPane>(window, "tabs").selectedIndex = 0
                find<JComboBox<*>>(window, "whitePlayerCombo").selectedItem = "Анна"
                find<JComboBox<*>>(window, "blackPlayerCombo").selectedItem = "Анна"
                find<JButton>(window, "startGameButton").doClick()
                assertTrue(find<JLabel>(window, "statusMessage").text.contains("разных игроков"))
                find<JComboBox<*>>(window, "blackPlayerCombo").selectedItem = "Борис"
                find<JButton>(window, "startGameButton").doClick()

                val board = find<BoardPanel>(window, "gameBoard")
                click(board, Point(4, 2))
                assertTrue(find<JLabel>(window, "statusMessage").text.contains("Выберите камень"))
                click(board, Point(2, 2))
                click(board, Point(3, 2))
                assertTrue(controller.boardState().canEndMove)
                find<JButton>(window, "endMoveButton").doClick()
                assertEquals(Color.BLACK, controller.boardState().currentSide)

                respondToDialog("Отмена партии") { JOptionPane.YES_OPTION }
                find<JButton>(window, "cancelGameButton").doClick()
                assertFalse(controller.boardState().active)
                assertEquals(2, controller.players().size)
                assertTrue(controller.history("Анна").isEmpty())
            }
        } finally {
            SwingUtilities.invokeAndWait { window.dispose() }
        }
    }

    @Test
    fun `ambiguous capture asks which capture to perform`() {
        val controller = controller(PositionFactory(".BW.B....\n.........\n.........\n.........\n........."))
        lateinit var window: SwingApplication
        SwingUtilities.invokeAndWait { window = SwingApplication(controller).apply { isVisible = true } }
        try {
            SwingUtilities.invokeAndWait {
                val name = find<JTextField>(window, "playerNameField")
                val register = find<JButton>(window, "registerPlayerButton")
                name.text = "Анна"
                register.doClick()
                name.text = "Борис"
                register.doClick()
                find<JComboBox<*>>(window, "whitePlayerCombo").selectedItem = "Анна"
                find<JComboBox<*>>(window, "blackPlayerCombo").selectedItem = "Борис"
                find<JButton>(window, "startGameButton").doClick()

                val board = find<BoardPanel>(window, "gameBoard")
                click(board, Point(2, 0))
                respondToDialog("Вид захвата") { pane ->
                    pane.options.first { it.toString().startsWith("Отступление") }
                }
                click(board, Point(3, 0))
                assertEquals(Color.WHITE, controller.boardState().stones[Point(3, 0)])
                assertFalse(Point(1, 0) in controller.boardState().stones)
                assertEquals(Color.BLACK, controller.boardState().stones[Point(4, 0)])
            }
        } finally {
            SwingUtilities.invokeAndWait { window.dispose() }
        }
    }

    private fun controller(factory: PositionFactory): DesktopController {
        val players = InMemoryPlayerRepository()
        val games = InMemoryGameRepository()
        return DesktopController(
            GameService(players, games, ActionService(factory.createBoard(), factory.createActionRules()), factory),
            PlayerRegistryService(players),
            StatisticsService(players, games),
            ReplayService(games, factory),
            factory,
        )
    }

    private fun click(board: BoardPanel, point: Point) {
        val x = (60.0 + point.x * (board.width - 120.0) / 8.0).toInt()
        val y = (60.0 + point.y * (board.height - 120.0) / 4.0).toInt()
        board.dispatchEvent(MouseEvent(board, MouseEvent.MOUSE_CLICKED, 0, 0, x, y, 1, false))
    }

    private fun respondToDialog(title: String, choice: (JOptionPane) -> Any) {
        SwingUtilities.invokeLater {
            val dialog = Window.getWindows().filterIsInstance<JDialog>()
                .first { it.isVisible && it.title == title }
            val pane = searchByType<JOptionPane>(dialog)
            pane.value = choice(pane)
        }
    }

    private inline fun <reified T : Component> find(root: Container, name: String): T {
        return search(root, name) as? T ?: error("Component not found: $name")
    }

    private fun search(component: Component, name: String): Component? {
        if (component.name == name) return component
        if (component is Container) {
            for (child in component.components) search(child, name)?.let { return it }
        }
        return null
    }

    private inline fun <reified T : Component> searchByType(root: Container): T =
        searchType(root, T::class.java) as? T ?: error("Component not found: ${T::class.java.name}")

    private fun searchType(component: Component, type: Class<*>): Component? {
        if (type.isInstance(component)) return component
        if (component is Container) {
            for (child in component.components) searchType(child, type)?.let { return it }
        }
        return null
    }
}
