package GameFlow.Core

import java.util.ArrayDeque

import GameFlow.Database.Repositories.ProgressRepository
import GameFlow.Models.GameType
import GameFlow.Models.Task
import GameFlow.Models.TaskResult
import GameFlow.Services.LoggingService

/**
 * FIFO task queue. The story/mission automation builds an ordered plan
 * (Open Mission -> Select -> Start -> Wait -> Claim -> Continue), executes items
 * in order and records outcomes/history. Progress feeds the dashboard.
 */
class TaskManager(
    private val log: LoggingService,
    private val source: String,
    private val history: ProgressRepository?,
    private val game: GameType) {

    private val pending: ArrayDeque<Task> = ArrayDeque()
    private var planned: Int = 0
    private var completed: Int = 0

    fun clear() {
        pending.clear()
        planned = 0
        completed = 0
    }

    fun plan(vararg tasks: Task) {
        clear()
        for (t in tasks) pending.add(t)
        planned = pending.size
        log.info(source, "Task queue ready: $planned tasks.")
    }

    fun next(): Task? = pending.pollFirst()

    fun complete(task: Task, result: TaskResult) {
        completed++
        log.info(source, "Task '${task.description}' -> $result")
        try {
            history?.recordTaskHistory(game.profileKey(), task.description, result.name)
        } catch (ignored: Throwable) { }
    }

    fun isEmpty(): Boolean = pending.isEmpty()
    fun completed(): Int = completed
    fun total(): Int = planned
}