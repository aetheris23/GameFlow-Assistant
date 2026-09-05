package GameFlow.Models

import java.util.concurrent.CopyOnWriteArrayList
import kotlin.jvm.Volatile
import GameFlow.Models.AutomationStatus

/**
 * Live status aggregate that the engine updates and the UI (View Models) observe.
 * This is the "Model" half of the MVVM-style layering: the engine never touches
 * Swing directly, it just mutates this snapshot and notifies subscribers.
 */
class DashboardModel {

    private val listeners: CopyOnWriteArrayList<(DashboardModel) -> Unit> = CopyOnWriteArrayList()

    private var game: GameType = GameType.NONE
    @Volatile private var gameStatus: String = "Waiting for game selection"
    @Volatile private var automation: AutomationStatus = AutomationStatus.IDLE
    @Volatile private var currentState: String = GameState.UNKNOWN.name
    @Volatile private var currentTask: String = "-"
    @Volatile private var completedTasks: Int = 0
    @Volatile private var totalTasks: Int = 0
    @Volatile private var errorStatus: String = ""

    fun subscribe(listener: (DashboardModel) -> Unit) { listeners.add(listener) }
    fun notifyChanged() { for (c in listeners) c(this) }

    fun game(): GameType = game
    fun gameStatus(): String = gameStatus
    fun automation(): AutomationStatus = automation
    fun currentState(): String = currentState
    fun currentTask(): String = currentTask
    fun completedTasks(): Int = completedTasks
    fun totalTasks(): Int = totalTasks
    fun errorStatus(): String = errorStatus

    fun setGame(g: GameType) { this.game = g; notifyChanged() }
    fun setGameStatus(s: String) { this.gameStatus = s; notifyChanged() }
    fun setAutomation(s: AutomationStatus) { this.automation = s; notifyChanged() }
    fun setCurrentState(s: String) { this.currentState = s; notifyChanged() }
    fun setCurrentTask(s: String) { this.currentTask = s; notifyChanged() }
    fun setProgress(completed: Int, total: Int) {
        this.completedTasks = completed; this.totalTasks = total; notifyChanged()
    }
    fun setErrorStatus(s: String) { this.errorStatus = s; notifyChanged() }
}