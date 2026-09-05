package GameFlow.UI

import java.awt.Dimension
import java.util.List
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.swing.JFrame
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.event.ChangeListener

import GameFlow.Database.Models.AppSettings
import GameFlow.Services.LoggingService

/**
 * Main application window (MVVM-style View). Hosts Dashboard / Settings / Logs
 * tabs and repaints them from the {@link GameFlow.Models.DashboardModel} via a
 * low-frequency Swing Timer (default 300 ms), never spinning the UI thread.
 */
class GameFlowFrame : JFrame {

    private val vm: MainViewModel
    private val log: LoggingService
    private val dashboard: DashboardPanel
    private val settings: SettingsPanel
    private val logs: LogsPanel
    private val timer: Timer

    constructor(vm: MainViewModel, log: LoggingService) : super("GameFlow Assistant") {
        this.vm = vm
        this.log = log

        setDefaultCloseOperation(EXIT_ON_CLOSE)
        setPreferredSize(Dimension(720, 480))

        dashboard = DashboardPanel(vm)
        settings = SettingsPanel()
        logs = LogsPanel()

        val tabs = JTabbedPane()
        tabs.addTab("Dashboard", dashboard)
        tabs.addTab("Settings", settings)
        tabs.addTab("Logs", logs)
        setContentPane(tabs)
        pack()

        // Repaint from the model on the EDT; the engine writes the model on its
        // own background thread, so there is never contention here.
        timer = Timer(300) { refresh() }
        timer.setRepeats(true)
        timer.start()

        // Clean shutdown on close.
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) { onExit() }
        })
    }

    private fun refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater({ refresh() })
            return
        }
        dashboard.refresh()
        logs.appendLine(log)
    }

    /** Called by Settings tab users to load a game's settings. */
    fun loadSettings(s: AppSettings) {
        settings.load(s)
    }

    /** Current settings values from the Settings tab. */
    fun settingsSnapshot(): AppSettings = settings.snapshot()

    /** Hooks a change listener onto settings controls. */
    fun onSettingsChanged(l: ChangeListener) {
        settings.apply(l)
    }

    private fun onExit() {
        timer.stop()
    }
}