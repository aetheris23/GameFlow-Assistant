package GameFlow.Models

/**
 * Poll cadence used by the adaptive scheduler to keep CPU low. The engine swaps
 * poll mode based on what the screen is doing rather than polling at a fixed
 * high frame rate.
 */
enum class PollMode {
    LOADING,   // ~1000 ms
    ACTIVE,    // ~300 ms
    IDLE,      // ~1500 ms
    PAUSED     // suspend screen processing while possible
}