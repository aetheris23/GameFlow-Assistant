package GameFlow.UI

import GameFlow.Models.AutomationStatus
import GameFlow.Models.GameType
import GameFlow.Models.DashboardModel

/**
 * ViewModel binding the engine to the UI (the "VM" in MVVM). The UI reads via
 * {@code snapshot()} and calls actions ({@code start}/{@code pause}...). The
 * engine only mutates the {@link DashboardModel}; a Swing Timer repaints from it,
 * so the UI thread and engine thread never contend.
 */
class MainViewModel(private val model: DashboardModel) {

    private var selectedGame: GameType = GameType.NONE
    private var logTail: String = ""

    // Actions wired by the App when the engine is created.
    private var onStart: () -> Unit = {}
    private var onPause: () -> Unit = {}
    private var onResume: () -> Unit = {}
    private var onStop: () -> Unit = {}
    private var onOpenSettings: (String) -> Unit = {}

    fun model(): DashboardModel = model

    fun bindEngineActions(start: () -> Unit, pause: () -> Unit, resume: () -> Unit,
                          stop: () -> Unit, openSettings: (String) -> Unit) {
        this.onStart = start
        this.onPause = pause
        this.onResume = resume
        this.onStop = stop
        this.onOpenSettings = openSettings
    }

    fun setSelectedGame(game: GameType) {
        this.selectedGame = game
        model.setGame(game)
    }

    fun selectedGame(): GameType = selectedGame

    fun start() { onStart() }
    fun pause() { onPause() }
    fun resume() { onResume() }
    fun stop() { onStop() }
    fun openSettings(gameKey: String) { onOpenSettings(gameKey) }

    fun setLogTail(t: String) { this.logTail = t }
    fun logTail(): String = logTail

    fun automation(): AutomationStatus = model.automation()
    fun running(): Boolean = automation() == AutomationStatus.RUNNING
    fun paused(): Boolean = automation() == AutomationStatus.PAUSED
}