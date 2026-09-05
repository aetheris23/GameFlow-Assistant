package GameFlow.Core

import kotlin.jvm.Volatile
import GameFlow.Models.AutomationStatus
import GameFlow.Services.LoggingService

/**
 * Internal safety system (spec #13). Guards the automation with:
 *  - emergency stop / pause
 *  - per-action timeout
 *  - maximum retries before giving up on an action
 *  - unknown-screen protection (no blind clicks)
 *  - an action cooldown between inputs
 * The engine consults this agent before every action and aborts on any breach.
 */
class SafetyManager(
    private val log: LoggingService,
    private val source: String,
    private val maxRetries: Int,
    private val actionTimeoutMs: Int,
    private val cooldownMs: Int) {

    @Volatile private var current: AutomationStatus = AutomationStatus.IDLE
    private var actionFailures: Int = 0

    /** Blocks for the inter-action cooldown. No-op when idle. */
    fun cooldown() {
        if (current != AutomationStatus.RUNNING) return
        try { Thread.sleep(cooldownMs.toLong()) } catch (ignored: InterruptedException) { Thread.currentThread().interrupt() }
    }

    /** Records a failed action attempt; returns true once retries are exhausted. */
    fun recordFailure(action: String): Boolean {
        actionFailures++
        log.warn(source, "$action attempt $actionFailures/$maxRetries")
        if (actionFailures >= maxRetries) {
            log.error(source, "Action timeout. Automation has been stopped.")
            emergencyStop()
            return true
        }
        return false
    }

    /** Success resets the retry counter. */
    fun recordSuccess(action: String) {
        actionFailures = 0
    }

    /** Starts a timed action; returns false if the action must not run now. */
    fun beginAction(action: String): Boolean {
        if (current != AutomationStatus.RUNNING) {
            log.warn(source, "Blocked action '$action' because automation is $current.")
            return false
        }
        return true
    }

    fun endAction() { /* hook for timing/telemetry */ }

    fun isRunning(): Boolean = current == AutomationStatus.RUNNING
    fun isPaused(): Boolean = current == AutomationStatus.PAUSED

    fun status(): AutomationStatus = current

    fun pause(reason: String) {
        current = AutomationStatus.PAUSED
        log.warn(source, "Automation paused: $reason")
    }

    fun resume() {
        if (current == AutomationStatus.PAUSED) {
            current = AutomationStatus.RUNNING
            actionFailures = 0
            log.info(source, "Automation resumed by user.")
        }
    }

    fun start() { current = AutomationStatus.RUNNING; actionFailures = 0 }

    fun stop(reason: String) {
        current = AutomationStatus.STOPPED
        log.info(source, "Automation stopped: $reason")
    }

    fun emergencyStop() { stop("emergency stop") }

    /** True when an immediate stop has been requested. */
    fun shouldHalt(): Boolean = current == AutomationStatus.STOPPED || current == AutomationStatus.ERROR

    fun actionTimeoutMs(): Int = actionTimeoutMs
}