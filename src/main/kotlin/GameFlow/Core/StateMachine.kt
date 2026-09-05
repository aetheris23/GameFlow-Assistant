package GameFlow.Core

import java.util.ArrayDeque
import GameFlow.Models.LogLevel
import GameFlow.Services.LoggingService
import GameFlow.Models.GameState

/**
 * Finite state machine over {@link GameState}. The engine never performs an
 * action without first consulting the current state; an UNKNOWN state forces a
 * halt. Keeps a short transition history for diagnostics.
 */
class StateMachine(private val log: LoggingService, private val source: String) {

    private var current: GameState = GameState.UNKNOWN
    private val history: ArrayDeque<GameState> = ArrayDeque()
    private var unknownStreak: Int = 0

    /** Transitions to a new state, logging the transition (info level). */
    fun transitionTo(next: GameState): GameState {
        val prev = current
        synchronized (this) {
            if (next == GameState.UNKNOWN) {
                unknownStreak++
            } else {
                unknownStreak = 0
            }
            current = next
            history.addLast(next)
            if (history.size > 20) history.removeFirst()
        }
        if (prev != next) {
            log.info(source, "State $prev -> $next")
        }
        if (next == GameState.UNKNOWN) {
            log.warn(source, "Unknown screen detected ($unknownStreak). Automation will pause.")
        }
        return next
    }

    fun get(): GameState = synchronized (this) { current }

    fun unknownStreak(): Int = synchronized (this) { unknownStreak }

    fun isUnknown(): Boolean = get() == GameState.UNKNOWN

    /** True when the current state is stable/safe enough for an action. */
    fun actionable(): Boolean {
        val s = get()
        return !isTransient(s)
    }

    private fun isTransient(s: GameState): Boolean =
        s == GameState.UNKNOWN || s == GameState.LOADING

    fun resetLog() {
        synchronized (this) {
            unknownStreak = 0
            current = GameState.UNKNOWN
            history.clear()
        }
        log.debug(source, "State machine reset to UNKNOWN")
    }

    fun lastStable(): GameState =
        synchronized (this) {
            var stable = GameState.UNKNOWN
            for (s in history) {
                if (s != GameState.UNKNOWN && s != GameState.LOADING) { stable = s; break }
            }
            stable
        }
}