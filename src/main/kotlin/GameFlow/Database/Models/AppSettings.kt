package GameFlow.Database.Models

import GameFlow.Models.FeatureType
import GameFlow.Models.PollMode

/**
 * Operational settings per game, mostly tunable numbers that influence the
 * adaptive scheduler and the safety manager. Loaded from AutomationSettings.
 */
data class AppSettings(
    val pollLoadingMs: Int,
    val pollActiveMs: Int,
    val pollIdleMs: Int,
    val actionCooldownMs: Int,
    val maxRetries: Int,
    val actionTimeoutMs: Int,
    val feature: FeatureType,
    val ocrEnabled: Boolean,
    val debugLogging: Boolean) {

    companion object {
        fun defaults(): AppSettings =
            AppSettings(1000, 300, 1500, 400, 5, 15000, FeatureType.STORY, false, false)
    }

    /** True when the user has never customized anything since the stock values. */
    fun isStockDefaults(): Boolean = this == defaults()

    fun pollMsFor(mode: PollMode): Int {
        return if (mode == PollMode.LOADING) pollLoadingMs
                else if (mode == PollMode.ACTIVE) pollActiveMs
                else if (mode == PollMode.IDLE) pollIdleMs
                else Int.MAX_VALUE    // suspend
    }
}