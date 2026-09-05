package GameFlow.Models

/** Progress line exposed to the dashboard (completed / total). */
class TaskProgress(val completed: Int, val total: Int) {
    companion object {
        fun empty(): TaskProgress = TaskProgress(0, 0)
    }
}