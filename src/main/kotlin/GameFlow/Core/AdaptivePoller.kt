package GameFlow.Core

import GameFlow.Models.PollMode

/**
 * Adaptive polling (spec #10). Chooses the check interval from the screen's
 * activity so CPU stays low: fast while active, slow while loading/idle, and
 * suspended entirely while paused. The engine asks this for how long to sleep
 * between captures.
 */
class AdaptivePoller(private val intervalFor: (PollMode) -> Int) {

    /** Sleeps for the interval bound to the given mode, honoring interrupts. */
    fun sleepFor(mode: PollMode) {
        if (mode == PollMode.PAUSED) return // suspended: engine waits on a latch instead
        val ms = intervalFor(mode)
        try { Thread.sleep(Math.max(0, ms).toLong()) } catch (ignored: InterruptedException) { Thread.currentThread().interrupt() }
    }

    companion object {
        /** Convenience state->speed mapping used by the engine. */
        fun modeFor(paused: Boolean, unknownLoading: Boolean): PollMode {
            if (paused) return PollMode.PAUSED
            return if (unknownLoading) PollMode.LOADING else PollMode.ACTIVE
        }
    }
}