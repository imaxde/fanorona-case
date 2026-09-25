package fanorona

import fanorona.persistence.SqliteDatabase
import fanorona.setup.ClassicFanoronaFactory
import fanorona.setup.GameSetupFactory
import fanorona.ui.ConsoleApplication
import fanorona.ui.DesktopController
import fanorona.ui.SwingApplication
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.InputStream
import java.io.PrintStream
import java.nio.file.Path
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main(args: Array<String>) {
    if (args.contentEquals(arrayOf("--help"))) {
        println("Режимы: --gui (по умолчанию), --console")
        println("Файл базы данных: --database=ПУТЬ")
        return
    }
    val databaseOptions = args.filter { it.startsWith("--database=") }
    require(databaseOptions.size <= 1) { "Укажите только один файл базы данных" }
    val databasePath = databaseOptions.singleOrNull()?.substringAfter('=')?.let {
        require(it.isNotBlank()) { "Укажите путь к файлу базы данных" }
        Path.of(it)
    } ?: defaultDatabasePath()
    when (args.filterNot { it.startsWith("--database=") }) {
        emptyList<String>(), listOf("--gui") -> runGui(ClassicFanoronaFactory(), databasePath)
        listOf("--console") -> runConsole(ClassicFanoronaFactory(), System.`in`, System.out, databasePath)
        else -> throw IllegalArgumentException("Неизвестные параметры. Используйте --help")
    }
}

fun defaultDatabasePath(
    environment: Map<String, String> = System.getenv(),
    userHome: Path = Path.of(System.getProperty("user.home")),
): Path {
    val dataHome = environment["XDG_DATA_HOME"]?.takeIf(String::isNotBlank)?.let(Path::of)
        ?.takeIf(Path::isAbsolute) ?: userHome.resolve(".local/share")
    return dataHome.resolve("fanorona-case/games.db")
}

fun runGui(factory: GameSetupFactory, databasePath: Path) {
    val database = SqliteDatabase(databasePath)
    val services = AppServices(database, factory)
    val controller = DesktopController(
        services.gameService, services.registry, services.statistics, services.replay, factory,
    )
    SwingUtilities.invokeLater {
        UIManager.getInstalledLookAndFeels().firstOrNull { it.name == "Nimbus" }?.let {
            UIManager.setLookAndFeel(it.className)
        }
        SwingApplication(controller).apply {
            addWindowListener(object : WindowAdapter() {
                override fun windowClosed(event: WindowEvent) = database.close()
            })
            isVisible = true
        }
    }
}

fun runConsole(
    factory: GameSetupFactory,
    input: InputStream,
    output: PrintStream,
    databasePath: Path,
) {
    SqliteDatabase(databasePath).use { database ->
        val services = AppServices(database, factory)
        ConsoleApplication(
            services.gameService, services.statistics, services.replay, factory, input, output,
        ).run()
    }
}
