package GameFlow.Models

/** Execution outcome of a single task item. */
enum class TaskResult {
    SUCCESS,
    FAILED,
    SKIPPED,
    TIMEOUT,
    BLOCKED   // e.g. paused by a user dialog choice
}