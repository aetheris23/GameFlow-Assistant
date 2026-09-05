package GameFlow.Models

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * A single user-facing log line, also persisted to SQLite. Immutable and cheap
 * to create; DEBUG entries can be dropped before construction to save work.
 */
class LogEntry(val timestamp: LocalTime, val level: LogLevel, val source: String, val message: String) {

    companion object {
        private val FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        /** A DEBUG suppression aid used by the logger before formatting. */
        fun of(level: LogLevel, source: String, message: String): LogEntry =
            LogEntry(LocalTime.now(), level, source, message)
    }

    override fun toString(): String = "[" + FMT.format(timestamp) + "] " + level + " " + message
}