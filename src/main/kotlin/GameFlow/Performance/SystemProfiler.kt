package GameFlow.Performance

import java.lang.management.ManagementFactory

import GameFlow.Models.PerformanceProfile

/**
 * Detects the host machine's capabilities without pulling in native libraries.
 *
 * <p>Because the low-resource requirement matters most on old laptops, the
 * profiler also samples recent system CPU load and exposes it (informational +
 * a tie-breaker in the classification). Memory/load values are read via
 * reflection so the same code works across JDK 17+ (the methods changed name
 * between JDK 14 and 20) and on non-HotSpot runtimes that throw.</p>
 */
class SystemProfiler {

    /**
     * Runs a quick (sub-second) probe of the machine and returns a
     * [HardwareProfile] with performance knobs scaled for it.
     */
    fun detect(): HardwareProfile {
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val totalMB = memory(FIELD_TOTAL)
        val freeMB = memory(FIELD_FREE)
        val load = sampleCpuLoad()

        val level = classify(cores, totalMB, freeMB, load)
        return HardwareProfile(level, cores, totalMB, freeMB, load)
    }

    private fun classify(cores: Int, totalMB: Long, freeMB: Long, load: Double): PerformanceProfile {
        val ramGB = memoryCapable(totalMB, freeMB)
        val weakCores = cores < 4
        val weakRam = ramGB in 1..7

        // Heavy current load tips a mid-class machine down one tier.
        val heavilyLoaded = load in 0.75..1.0

        return when {
            weakCores || weakRam -> PerformanceProfile.LOW
            cores < 8 || (!heavilyLoaded && ramGB < 16) -> PerformanceProfile.MEDIUM
            else -> PerformanceProfile.HIGH
        }
    }

    /** Effective usable RAM in GB; 0 when nothing could be measured. */
    private fun memoryCapable(totalMB: Long, freeMB: Long): Int {
        val capacity = if (totalMB > 0) totalMB else freeMB
        return (capacity / 1024).toInt()
    }

    /** Reads a physical-memory figure via reflection (0 when unavailable). */
    private fun memory(fields: Pair<String, String>): Long {
        val value = osBeanNumber(fields.first, fields.second) ?: return 0L
        // com.sun.management reports bytes; some runtimes report KB. Normalise.
        return (value / (1024 * 1024)).toLong()
    }

    /** A short average of system CPU load in [0,1]; -1 when not measurable. */
    private fun sampleCpuLoad(): Double {
        val s1 = readCpuLoad()
        if (s1 < 0) return -1.0
        try { Thread.sleep(120) } catch (e: InterruptedException) { Thread.currentThread().interrupt() }
        val s2 = readCpuLoad()
        if (s2 < 0) return s1
        val avg = (s1 + s2) / 2.0
        return avg.coerceIn(0.0, 1.0)
    }

    private fun readCpuLoad(): Double =
        // Prefer the per-system figure; fall back to the process load.
        osBeanNumber("getSystemCpuLoad", "getProcessCpuLoad") ?: -1.0

    /**
     * Invokes a method on the OS bean by trying the given names in order.
     * Handles JDK 14 (getTotalMemorySize + getAvailableMemory) and the older
     * ones (getTotalPhysicalMemorySize / getFreePhysicalMemorySize) transparently.
     */
    private fun osBeanNumber(primary: String, fallback: String): Double? {
        val bean = ManagementFactory.getOperatingSystemMXBean()
        for (name in listOf(primary, fallback)) {
            try {
                val m = bean.javaClass.getMethod(name)
                val v = m.invoke(bean)
                if (v is Number) return v.toDouble()
            } catch (ignored: Throwable) {
                // method missing/uninvokable -> try next
            }
        }
        return null
    }

    companion object {
        private val FIELD_TOTAL = "getTotalMemorySize" to "getTotalPhysicalMemorySize"
        private val FIELD_FREE = "getAvailableMemory" to "getFreePhysicalMemorySize"
    }
}