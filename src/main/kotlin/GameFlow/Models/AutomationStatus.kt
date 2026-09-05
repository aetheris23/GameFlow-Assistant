package GameFlow.Models

/** Lifecycle status of the whole automation run / UI. */
enum class AutomationStatus {
    IDLE,
    RUNNING,
    PAUSED,
    STOPPED,
    COMPLETED,
    ERROR
}