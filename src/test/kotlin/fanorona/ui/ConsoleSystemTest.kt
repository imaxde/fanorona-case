package fanorona.ui

import fanorona.domain.Arrangement
import fanorona.domain.Board
import fanorona.domain.Color
import fanorona.domain.Point
import fanorona.domain.Stone
import fanorona.runConsole
import fanorona.main
import fanorona.setup.ClassicFanoronaFactory
import fanorona.setup.PositionFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConsoleSystemTest {
    @Test
    fun `command parser supports names with spaces and rejects unfinished quotes`() {
        assertEquals(
            listOf("start", "Анна Иванова", "Борис"),
            CommandParser.parse("start \"Анна Иванова\" Борис"),
        )
        assertEquals(listOf("stats", "Аня \"Белая\""), CommandParser.parse("stats \"Аня \\\"Белая\\\"\""))
        assertFailsWith<IllegalArgumentException> { CommandParser.parse("start \"Аня") }
    }

    @Test
    fun `board renderer labels coordinates and displays stones`() {
        val arrangement = Arrangement(mapOf(Point(0, 0) to Stone(Color.WHITE), Point(8, 4) to Stone(Color.BLACK)))
        val result = BoardRenderer.render(arrangement, Board())

        assertContains(result, "0   1   2   3   4   5   6   7   8")
        assertContains(result, "0  W---.")
        assertContains(result, "4  .---.---.---.---.---.---.---.---B")
        assertContains(result, "| \\ | / |")
    }

    @Test
    fun `console rejects illegal opening action then cancels the active game`() {
        val output = runScript(
            ClassicFanoronaFactory(),
            "help\nstart Аня Боб\nboard\nactions\nmove paika 3,2 4,2\nstatus\ncancel\nplayers\nquit\n",
        )

        assertContains(output, "Доступен обязательный захват")
        assertContains(output, "Белые: 22")
        assertContains(output, "Партия отменена")
        assertContains(output, "Аня")
        assertContains(output, "Боб")
    }

    @Test
    fun `console completes a game and exposes history statistics and replay`() {
        val output = runScript(
            PositionFactory("W.B......\n.........\n.........\n.........\n........."),
            "start Аня Боб\nmove approach 0,0 1,0\nstats Аня\nhistory Аня\nreplay 1\nnext\nback\nquit\n",
        )

        assertContains(output, "Победа белых")
        assertContains(output, "победы: 1")
        assertContains(output, "Партия #1")
        assertContains(output, "Шаг 1")
        assertContains(output, "Шаг 0")
    }

    @Test
    fun `console can end a capture series early`() {
        val output = runScript(
            PositionFactory("...B.....\n.........\n..W.B....\n.........\n........."),
            "start Аня Боб\nmove approach 2,2 3,2\nstatus\nend\nstatus\nquit\n",
        )

        assertContains(output, "Снято в текущем ходе: 1")
        assertContains(output, "Ход: чёрные")
    }

    @Test
    fun `main starts the interactive application`() {
        val originalInput = System.`in`
        val originalOutput = System.out
        val bytes = ByteArrayOutputStream()
        try {
            System.setIn(ByteArrayInputStream("help\nquit\n".toByteArray(StandardCharsets.UTF_8)))
            System.setOut(PrintStream(bytes, true, StandardCharsets.UTF_8))

            main()

            assertContains(bytes.toString(StandardCharsets.UTF_8), "start <белые> <чёрные>")
        } finally {
            System.setIn(originalInput)
            System.setOut(originalOutput)
        }
    }

    private fun runScript(factory: fanorona.setup.GameSetupFactory, script: String): String {
        val bytes = ByteArrayOutputStream()
        val output = PrintStream(bytes, true, StandardCharsets.UTF_8)
        runConsole(factory, ByteArrayInputStream(script.toByteArray(StandardCharsets.UTF_8)), output)
        return bytes.toString(StandardCharsets.UTF_8)
    }
}
