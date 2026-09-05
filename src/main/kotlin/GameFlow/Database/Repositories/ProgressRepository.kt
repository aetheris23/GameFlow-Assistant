package GameFlow.Database.Repositories

import java.sql.Connection
import java.sql.SQLException
import java.time.LocalDateTime

import GameFlow.Database.DatabaseException

/** Story + mission progress persistence. */
class ProgressRepository(private val db: Connection) {

    fun saveStory(gameKey: String, episode: Int, step: Int) {
        val ps = db.prepareStatement("""
            INSERT INTO StoryProgress(game_key, episode, step, updated_at)
            VALUES(?,?,?,?)
            ON CONFLICT(game_key) DO UPDATE SET episode=excluded.episode,
                step=excluded.step, updated_at=excluded.updated_at
            """)
        try {
            ps.setString(1, gameKey)
            ps.setInt(2, episode)
            ps.setInt(3, step)
            ps.setString(4, LocalDateTime.now().toString())
            ps.executeUpdate()
        } catch (e: SQLException) {
            throw dbEx(e)
        } finally {
            ps.close()
        }
    }

    fun loadStory(gameKey: String): IntArray {
        val ps = db.prepareStatement("SELECT episode, step FROM StoryProgress WHERE game_key=?")
        try {
            ps.setString(1, gameKey)
            val rs = ps.executeQuery()
            try {
                if (rs.next()) return intArrayOf(rs.getInt("episode"), rs.getInt("step"))
                return intArrayOf(0, 0)
            } finally {
                rs.close()
            }
        } catch (e: SQLException) {
            throw dbEx(e)
        } finally {
            ps.close()
        }
    }

    fun saveMission(gameKey: String, missionId: Int, state: String) {
        val ps = db.prepareStatement("""
            INSERT INTO MissionProgress(game_key, mission_id, state, updated_at)
            VALUES(?,?,?,?)
            ON CONFLICT(game_key) DO UPDATE SET mission_id=excluded.mission_id,
                state=excluded.state, updated_at=excluded.updated_at
            """)
        try {
            ps.setString(1, gameKey)
            ps.setInt(2, missionId)
            ps.setString(3, state)
            ps.setString(4, LocalDateTime.now().toString())
            ps.executeUpdate()
        } catch (e: SQLException) {
            throw dbEx(e)
        } finally {
            ps.close()
        }
    }

    /** Pushes a completed mission task into the history table. */
    fun recordTaskHistory(gameKey: String, taskName: String, result: String) {
        val ps = db.prepareStatement("INSERT INTO TaskHistory(game_key, task_name, result, created_at) VALUES(?,?,?,?)")
        try {
            ps.setString(1, gameKey)
            ps.setString(2, taskName)
            ps.setString(3, result)
            ps.setString(4, LocalDateTime.now().toString())
            ps.executeUpdate()
        } catch (e: SQLException) {
            throw dbEx(e)
        } finally {
            ps.close()
        }
    }

    private fun dbEx(e: SQLException): DatabaseException = DatabaseException("Progress query failed", e)
}