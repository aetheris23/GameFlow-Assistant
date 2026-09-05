package GameFlow

import java.nio.file.Files
import java.nio.file.Path

import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.JFrame
import javax.swing.WindowConstants
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import GameFlow.Core.AdaptivePoller
import GameFlow.Core.AutomationEngine
import GameFlow.Core.SafetyManager
import GameFlow.Core.StateMachine
import GameFlow.Database.DatabaseContext
import GameFlow.Database.Repositories.SettingsRepository
import GameFlow.Games.GameSessionFactory
import GameFlow.Input.MouseController
import GameFlow.Models.AutomationStatus
import GameFlow.Models.DashboardModel
import GameFlow.Models.GameType
import GameFlow.Performance.HardwareProfile
import GameFlow.Performance.SystemProfiler
import GameFlow.Services.GameWindowDetector
import GameFlow.Services.LoggingService
import GameFlow.UI.GameFlowFrame
import GameFlow.UI.GameSelectionDialog
import GameFlow.UI.MainViewModel
import GameFlow.Vision.TemplateMatchers

/**
 * Application entry point. Boots the database, the dashboard model, the logger,
 * detects the selected game, and wires the engine + Swing view together (MVVM:
 * the engine mutates the model; a Swing timer repaints the views from it).
 *
 * Flags:
 *   --demo     immediately selects Uma Musume (no dialog)
 *   --headless run the engine without opening the window (CI self-test)
 */
class App {

    fun start(demo: Boolean, headless: Boolean) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) } catch (ignored: Throwable) { }

        // Detect the host machine once at startup so the vision pipeline and
        // polling cadence are scaled (LOW/MEDIUM/HIGH) for its capabilities.
        val perf: HardwareProfile = SystemProfiler().detect()

        val dbPath = dataDir().resolve("gameflow.db")
        val dbParent = dbPath.getParent()
        if (dbParent != null) Files.createDirectories(dbParent)
        val db = DatabaseContext(dbPath).open()
        val settingsRepo = SettingsRepository(db.connection())
        settingsRepo.ensureGameRows()

        val dash = DashboardModel()
        val log = LoggingService(200)
        log.attach(db)

        val vm = MainViewModel(dash)

        val game = if (demo) GameType.UMA_MUSUME else promptGameSelection()
        if (game == GameType.NONE) {
            log.info("App", "No game selected. Exiting.")
            db.close()
            return
        }
        val gameKey = game.profileKey()
        var settings = settingsRepo.load(gameKey)

        // Apply hardware-tuned polling on first run; keep any later edits.
        if (settings.isStockDefaults()) {
            settings = perf.recommendedSettings()
            settingsRepo.save(gameKey, settings)
            log.info("Hardware", "Profile " + perf.summarize() + " -> tuned poll cadence applied for " + game.displayName() + ".")
        } else {
            log.info("Hardware", "Profile " + perf.summarize() + "; keeping user settings for " + game.displayName() + ".")
        }
        log.setDebugEnabled(settings.debugLogging)
        log.info("App", "Selected game: " + game.displayName())

        val mouse = MouseController()
        val matcher = TemplateMatchers.createDefault(perf.matcherScaleDown(), false)
        val session = GameSessionFactory.create(game, matcher, mouse, settings.actionCooldownMs)

        val detector = GameWindowDetector(log)
        val coreState = StateMachine(log, "gameflow")
        val safety = SafetyManager(log, "gameflow", settings.maxRetries,
            settings.actionTimeoutMs, settings.actionCooldownMs)
        val poller = AdaptivePoller { mode -> settings.pollMsFor(mode) }
        val engine = AutomationEngine(session, coreState, safety, poller, dash, log, "gameflow",
            perf.matcherScaleDown(), perf.changeSampleStride())

        val ensureWindow: () -> Unit = {
            val detected = detector.detect(game)
            if (detected != null) {
                session.attachWindow(detected)
                dash.setGameStatus("Window detected: $detected")
            }
        }
        vm.bindEngineActions(
            { ensureWindow(); engine.start() },
            { engine.pause() },
            { engine.resume() },
            { engine.stop() },
            {})
        registerShortcuts(engine)

        vm.setSelectedGame(game)

        if (headless) {
            headlessRun(engine, log, db)
            return
        }

        val frame = GameFlowFrame(vm, log)
        frame.setLocationRelativeTo(null)
        frame.loadSettings(settings)
        frame.onSettingsChanged {
            val updated = frame.settingsSnapshot()
            settingsRepo.save(gameKey, updated)
            settings = updated
            log.setDebugEnabled(updated.debugLogging)
            log.info("App", "Settings updated (feature=" + updated.feature + ").")
        }
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) { engine.requestShutdown() }
        })
        frame.setVisible(true)
    }

    private fun promptGameSelection(): GameType {
        val owner = JFrame("GameFlow Assistant - Game Selection")
        owner.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        val dialog = GameSelectionDialog(owner)
        dialog.setVisible(true)
        return dialog.chosenGame()
    }

    private fun headlessRun(engine: AutomationEngine, log: LoggingService, db: DatabaseContext) {
        engine.start()
        try { Thread.sleep(2500) } catch (e: InterruptedException) { Thread.currentThread().interrupt() }
        engine.stop()
        for (e in log.recent()) System.out.println(e)
        db.close()
    }

    companion object {
        /** App data location (user home); the SQLite DB lives here. */
        fun dataDir(): Path = Path.of(System.getProperty("user.home", "."), "GameFlowAssistant")

        @JvmStatic
        fun main(args: Array<String>) {
            val demo = args.toList().contains("--demo")
            val headless = args.toList().contains("--headless")
            val app = App()
            SwingUtilities.invokeLater({
                try {
                    app.start(demo, headless)
                } catch (t: Throwable) {
                    System.err.println("GameFlow Assistant startup failed: $t")
                    t.printStackTrace()
                    System.exit(1)
                }
            })
        }

        /**
         * Placeholder cross-platform hotkeys (spec #16: Pause/Resume/Stop).
         * A real desktop build would bind these via JNA/JavaFX shortcuts.
         */
        private fun registerShortcuts(engine: AutomationEngine) {
            // No-op
        }
    }
}
