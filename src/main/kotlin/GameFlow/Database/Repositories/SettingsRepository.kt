package GameFlow.Database.Repositories

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDateTime

import GameFlow.Database.DatabaseException
import GameFlow.Database.Models.AppSettings
import GameFlow.Models.FeatureType
import GameFlow.Models.GameType
import GameFlow.Models.LogLevel

/**
 * Persistence for settings and logs. Each DAO takes the day's Connection; the
 * engine calls them from a background worker so the UI thread never blocks.
 */
class SettingsRepository(private val db: Connection) {

    fun ensureGameRows() {
        for (g in listOf(GameType.UMA_MUSUME, GameType.BLUE_ARCHIVE)) {
            upsertGame(g)
            upsertSettingsDefaults(g)
        }
    }

    private fun upsertGame(g: GameType) {
        val ps = db.prepareStatement("INSERT OR IGNORE INTO Games(key, name, selected) VALUES(?,?,0)")
        try {
            ps.setString(1, g.profileKey())
            ps.setString(2, g.displayName())
            ps.executeUpdate()
        } catch (e: SQLException) {
            throw dbEx(e)
        } finally {
            ps.close()
        }
    }

    private fun upsertSettingsDefaults(g: GameType) {
        val s = AppSettings.defaults()
        val ps = db.prepareStatement("""
            INSERT INTO AutomationSettings
                (game_key, poll_loading_ms, poll_active_ms, poll_idle_ms,
                 action_cooldown_ms, max_retries, action_timeout_ms,
                 feature, ocr_enabled, debug_logging)
            VALUES(?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(game_key) DO NOTHING
            """)
        try {
            ps.setString(1, g.profileKey())
            ps.setInt(2, s.pollLoadingMs)
            ps.setInt(3, s.pollActiveMs)
            ps.setInt(4, s.pollIdleMs)
            ps.setInt(5, s.actionCooldownMs)
            ps.setInt(6, s.maxRetries)
            ps.setInt(7, s.actionTimeoutMs)
            ps.setString(8, "STORY")
            ps.setInt(9, if (s.ocrEnabled) 1 else 0)
            ps.setInt(10, if (s.debugLogging) 1 else 0)
            ps.executeUpdate()
        } catch (e: SQLException) {
            throw dbEx(e)
        } finally {
            ps.close()
        }
    }

    fun load(gameKey: String): AppSettings {
        val ps = db.prepareStatement("""
            SELECT poll_loading_ms, poll_active_ms, poll_idle_ms,
                   action_cooldown_ms, max_retries, action_timeout_ms,
                   feature, ocr_enabled, debug_logging
               FROM AutomationSettings WHERE game_key=? """)
        try {
            ps.setString(1, gameKey)
            val rs = ps.executeQuery()
            try {
                if (!rs.next()) return AppSettings.defaults()
                return AppSettings(
                    rs.getInt("poll_loading_ms"),
                    rs.getInt("poll_active_ms"),
                    rs.getInt("poll_idle_ms"),
                    rs.getInt("action_cooldown_ms"),
                    rs.getInt("max_retries"),
                    rs.getInt("action_timeout_ms"),
                    FeatureType.valueOf(rs.getString("feature")),
                    rs.getInt("ocr_enabled") == 1,
                    rs.getInt("debug_logging") == 1)
            } finally {
                rs.close()
            }
        } catch (e: SQLException) {
            throw dbEx(e)
        } finally {
            ps.close()
        }
    }

    fun save(gameKey: String, s: AppSettings) {
        val ps = db.prepareStatement("""
            UPDATE AutomationSettings SET
                poll_loading_ms=?, poll_active_ms=?, poll_idle_ms=?,
                action_cooldown_ms=?, max_retries=?, action_timeout_ms=?,
                feature=?, ocr_enabled=?, debug_logging=?
              WHERE game_key=?
            """)
        try {
            ps.setInt(1, s.pollLoadingMs)
            ps.setInt(2, s.pollActiveMs)
            ps.setInt(3, s.pollIdleMs)
            ps.setInt(4, s.actionCooldownMs)
            ps.setInt(5, s.maxRetries)
            ps.setInt(6, s.actionTimeoutMs)
            ps.setString(7, s.feature.name)
            ps.setInt(8, if (s.ocrEnabled) 1 else 0)
            ps.setInt(9, if (s.debugLogging) 1 else 0)
            ps.setString(10, gameKey)
            ps.executeUpdate()
        } catch (e: SQLException) {
            throw dbEx(e)
        } finally {
            ps.close()
        }
    }

    private fun dbEx(e: SQLException): DatabaseException = DatabaseException("Settings query failed", e)

    // ---- Logs ----

    fun insertLog(level: LogLevel, source: String, message: String) {
        val ps = db.prepareStatement("INSERT INTO Logs(level, source, message, created_at) VALUES(?,?,?,?)")
        try {
            ps.setString(1, level.name)
            ps.setString(2, source)
            ps.setString(3, message)
            ps.setString(4, LocalDateTime.now().toString())
            ps.executeUpdate()
        } catch (ignored: SQLException) {
            // Log persistence must never break the automation loop.
        } finally {
            ps.close()
        }
    }
}