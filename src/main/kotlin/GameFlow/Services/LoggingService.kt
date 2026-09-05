package GameFlow.Services

import java.util.ArrayDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import kotlin.jvm.Volatile
import GameFlow.Database.DatabaseContext
import GameFlow.Database.Repositories.SettingsRepository
import GameFlow.Models.LogEntry
import GameFlow.Models.LogLevel

/**
 * Thread-safe logger. {@code warn/error/info} always pass through; DEBUG entries
 * are suppressed while debug logging is off (default) to save resources. Logs
 * are persisted to SQLite by a single daemon flusher thread through a bounded
 * queue (never one thread per entry), and pushed to UI subscribers on a small
 * ring buffer so the dashboard gets a live view.
 */
class LoggingService(private val recentCapacity: Int) {

    private val ringCapacity: Int
    private val recent: ArrayDeque<LogEntry> = ArrayDeque()
    private val listeners: CopyOnWriteArrayList<(LogEntry) -> Unit> = CopyOnWriteArrayList()
    @Volatile private var debugEnabled: Boolean = false
    private var db: DatabaseContext? = null
    private var settings: SettingsRepository? = null

    private val flushQueue: LinkedBlockingQueue<LogEntry> = LinkedBlockingQueue(512)
    private val flusher: Thread

    init {
        this.ringCapacity = Math.max(50, recentCapacity)
        this.flusher = Thread({ drain() }, "gameflow-log-flusher")
        flusher.setDaemon(true)
        flusher.start()
    }

    fun attach(db: DatabaseContext?) {
        this.db = db
        this.settings = if (db != null) SettingsRepository(db.connection()) else null
    }

    fun setDebugEnabled(on: Boolean) { this.debugEnabled = on }
    fun debugEnabled(): Boolean = debugEnabled

    fun info(source: String, message: String) { emit(LogLevel.INFO, source, message) }
    fun warn(source: String, message: String) { emit(LogLevel.WARNING, source, message) }
    fun error(source: String, message: String) { emit(LogLevel.ERROR, source, message) }
    fun debug(source: String, message: String) {
        if (debugEnabled) emit(LogLevel.DEBUG, source, message)
    }

    fun recent(): List<LogEntry> = synchronized (recent) { recent.toList() }

    fun subscribe(listener: (LogEntry) -> Unit) { listeners.addIfAbsent(listener) }

    private fun emit(level: LogLevel, source: String, message: String) {
        val entry = LogEntry.of(level, source, message)
        synchronized (recent) {
            recent.addLast(entry)
            while (recent.size > ringCapacity) recent.removeFirst()
        }
        if (db != null && !flushQueue.offer(entry)) {
            // Queue is full: drop the oldest entry to bound memory instead of
            // falling behind on a slow disk.
            flushQueue.poll()
            flushQueue.offer(entry)
        }
        for (c in listeners) c(entry)
    }

    /** Single persistent flusher; keeps SQLite traffic off the caller's thread. */
    private fun drain() {
        while (true) {
            try {
                val entry = flushQueue.poll(2, TimeUnit.SECONDS)
                if (entry != null) persist(entry)
            } catch (ignored: Throwable) {
                // daemon must never die and never break the app
            }
        }
    }

    private fun persist(entry: LogEntry) {
        try {
            settings?.insertLog(entry.level, entry.source, entry.message)
        } catch (ignored: Throwable) {
            // Persistence must never break the loop.
        }
    }
}