package GameFlow.Models

/**
 * A concrete step in the task queue. A TaskManager executes items in FIFO order,
 * assigning results and guarding against runaway retries.
 */
class Task(val id: String, val description: String, val feature: FeatureType, val expectedState: GameState) {
    override fun toString(): String = if (description != null) description else id
}