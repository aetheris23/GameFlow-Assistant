package GameFlow.Performance

import GameFlow.Database.Models.AppSettings
import GameFlow.Models.FeatureType
import GameFlow.Models.PerformanceProfile

/**
 * Snapshot of the host's capabilities plus the performance knobs derived from
 * them. The [level] decides how aggressively the app polls, captures, and scans,
 * so a weak laptop never pegs its CPU while a strong desktop stays responsive.
 */
class HardwareProfile(
    /** Capability tier (LOW / MEDIUM / HIGH). */
    val level: PerformanceProfile,
    /** Logical CPU cores visible to the JVM. */
    val cores: Int,
    /** Total physical RAM in MB (0 when unknown). */
    val totalMemoryMB: Long,
    /** Free physical RAM in MB (0 when unknown). */
    val freeMemoryMB: Long,
    /** Recent system CPU usage in [0,1] (or -1 when unavailable). */
    val recentCpuLoad: Double) {

    /** How much to down-sample the pure-Java matcher on this machine. */
    fun matcherScaleDown(): Int = when (level) {
        PerformanceProfile.LOW -> 3
        PerformanceProfile.MEDIUM -> 2
        PerformanceProfile.HIGH -> 2
    }

    /**
     * Pixel stride for the change-detector comparison. Higher = cheaper checks
     * on large screens; 1 = compare every pixel.
     */
    fun changeSampleStride(): Int = when (level) {
        PerformanceProfile.LOW -> 4
        PerformanceProfile.MEDIUM -> 2
        PerformanceProfile.HIGH -> 1
    }

    /** Scales the polling cadence so weak boxes are polled much less often. */
    fun recommendedSettings(): AppSettings = when (level) {
        PerformanceProfile.HIGH -> AppSettings(
            pollLoadingMs = 1000, pollActiveMs = 300, pollIdleMs = 1500,
            actionCooldownMs = 400, maxRetries = 5, actionTimeoutMs = 15000,
            feature = FeatureType.STORY, ocrEnabled = false, debugLogging = false)
        PerformanceProfile.MEDIUM -> AppSettings(
            pollLoadingMs = 1600, pollActiveMs = 600, pollIdleMs = 2500,
            actionCooldownMs = 400, maxRetries = 5, actionTimeoutMs = 15000,
            feature = FeatureType.STORY, ocrEnabled = false, debugLogging = false)
        PerformanceProfile.LOW -> AppSettings(
            pollLoadingMs = 3000, pollActiveMs = 1200, pollIdleMs = 5000,
            actionCooldownMs = 400, maxRetries = 5, actionTimeoutMs = 15000,
            feature = FeatureType.STORY, ocrEnabled = false, debugLogging = false)
    }

    fun summarize(): String {
        val sb = StringBuilder()
        sb.append(level.name).append(" (")
        sb.append(cores).append(" cores, ")
        sb.append(totalMemoryMB / 1024).append(" GB RAM")
        if (recentCpuLoad in 0.0..1.0) sb.append(", cpu ~").append(Math.round(recentCpuLoad * 100)).append("%")
        sb.append(")")
        return sb.toString()
    }

    override fun toString(): String = summarize()
}