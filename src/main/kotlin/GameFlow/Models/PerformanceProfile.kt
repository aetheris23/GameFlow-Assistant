package GameFlow.Models

/**
 * Overall capability tier detected for the host machine. Higher tiers allow the
 * vision pipeline to capture/scan at higher frequency and resolution; lower
 * tiers trade responsiveness for keeping CPU/RAM flat on weak hardware.
 */
enum class PerformanceProfile {
    LOW,      // weak CPUs / small RAM -> conservative polling and matching
    MEDIUM,   // everyday machines -> balanced
    HIGH      // desktop/server-grade -> full rate
}