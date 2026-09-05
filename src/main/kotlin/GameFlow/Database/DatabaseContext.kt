package GameFlow.Database

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Owns the SQLite connection and schema. All data is stored locally; no server
 * or external database is required. The DB file lives next to the app data dir.
 */
class DatabaseContext(private val dbFile: Path) : AutoCloseable {

    private var connection: Connection? = null

    /** Opens (creating the file + schema if needed) and returns this context. */
    fun open(): DatabaseContext {
        try {
            val parent = dbFile.getParent()
            if (parent != null) Files.createDirectories(parent)
            val url = "jdbc:sqlite:" + dbFile.toAbsolutePath()
            val conn = DriverManager.getConnection(url)
            connection = conn
            val st = conn.createStatement()
            try { st.execute("PRAGMA journal_mode=WAL") } finally { st.close() }
            initSchema(conn)
            return this
        } catch (e: SQLException) {
            throw DatabaseException("Failed to open SQLite database at " + dbFile, e)
        } catch (e: IOException) {
            throw DatabaseException("Failed to open SQLite database at " + dbFile, e)
        }
    }

    fun connection(): Connection {
        val c = connection
        if (c == null) throw DatabaseException("Database not opened", null)
        return c
    }

    private fun initSchema(db: Connection) {
        val st = db.createStatement()
        try {
            // Core tables from the spec.
            st.execute("""
                CREATE TABLE IF NOT EXISTS Games (
                    game_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    key       TEXT NOT NULL UNIQUE,
                    name      TEXT NOT NULL,
                    selected  INTEGER NOT NULL DEFAULT 0
                )
                """)
            st.execute("""
                CREATE TABLE IF NOT EXISTS GameProfiles (
                    profile_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    game_key     TEXT NOT NULL,
                    reference_w  INTEGER NOT NULL,
                    reference_h  INTEGER NOT NULL,
                    jira_id      TEXT NOT NULL
                )
                """)
            st.execute("""
                CREATE TABLE IF NOT EXISTS AutomationSettings (
                    game_key            TEXT PRIMARY KEY,
                    poll_loading_ms     INTEGER NOT NULL DEFAULT 1000,
                    poll_active_ms      INTEGER NOT NULL DEFAULT 300,
                    poll_idle_ms        INTEGER NOT NULL DEFAULT 1500,
                    action_cooldown_ms  INTEGER NOT NULL DEFAULT 400,
                    max_retries         INTEGER NOT NULL DEFAULT 5,
                    action_timeout_ms   INTEGER NOT NULL DEFAULT 15000,
                    feature             TEXT NOT NULL DEFAULT 'STORY',
                    ocr_enabled         INTEGER NOT NULL DEFAULT 0,
                    debug_logging       INTEGER NOT NULL DEFAULT 0
                )
                """)
            st.execute("""
                CREATE TABLE IF NOT EXISTS StoryProgress (
                    game_key     TEXT NOT NULL,
                    episode      INTEGER NOT NULL DEFAULT 0,
                    step         INTEGER NOT NULL DEFAULT 0,
                    updated_at   TEXT NOT NULL
                )
                """)
            st.execute("""
                CREATE TABLE IF NOT EXISTS MissionProgress (
                    game_key     TEXT NOT NULL,
                    mission_id   INTEGER NOT NULL DEFAULT 0,
                    state        TEXT NOT NULL DEFAULT 'TODO',
                    updated_at   TEXT NOT NULL
                )
                """)
            st.execute("""
                CREATE TABLE IF NOT EXISTS Tasks (
                    task_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    game_key     TEXT NOT NULL,
                    feature      TEXT NOT NULL,
                    name         TEXT NOT NULL,
                    status       TEXT NOT NULL DEFAULT 'PENDING',
                    started_at   TEXT,
                    finished_at  TEXT
                )
                """)
            st.execute("""
                CREATE TABLE IF NOT EXISTS TaskHistory (
                    history_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    game_key     TEXT NOT NULL,
                    task_name    TEXT NOT NULL,
                    result       TEXT NOT NULL,
                    created_at   TEXT NOT NULL
                )
                """)
            st.execute("""
                CREATE TABLE IF NOT EXISTS Logs (
                    log_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    level    TEXT NOT NULL,
                    source   TEXT NOT NULL,
                    message  TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
                """)
        } finally {
            st.close()
        }
    }

    override fun close() {
        val c = connection
        if (c != null) {
            try { c.close() } catch (ignored: SQLException) { }
            connection = null
        }
    }
}