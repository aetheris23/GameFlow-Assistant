package GameFlow.Services

import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.PointerInfo

import GameFlow.Models.GameType
import GameFlow.Models.GameWindowInfo
import GameFlow.Models.Point
import GameFlow.Models.Rect
import GameFlow.Services.LoggingService

/**
 * Window detection (spec #2). On a real Windows setup this would be backed by
 * the Win32/Windows API to find the exact game window, retrieve its bounds, and
 * match the process title against the selected game (a "isRunning + title -> game"
 * heuristic).
 *
 * The implementation honestly documents that live native window enumeration
 * requires a small platform module. As shipped, the demo detector reports on the
 * primary display as a stand-in so the entire pipeline (capture -> classify ->
 * act) can be exercised end-to-end on low-resources, in headless CI, or in a
 * sandbox. Replace {@link #detect(GameType)} with a real HWND-based
 * implementation for production (see README / GameDetector).
 */
class GameWindowDetector(private val log: LoggingService) {

    /**
     * Detects the game window for the selected game. If found, returns a
     * {@link GameWindowInfo}; otherwise returns null.
     *
     * <p>MVP demo behavior: the detector keys on the primary display and always
     * claims the whole screen as the "window". A real deployment must:
     * </p>
     * <ol>
     *   <li>Enumerate top-level windows via the OS API.</li>
     *   <li>Match window titles/process to the chosen {@link GameType}.</li>
     *   <li>Read the window's client rect and map it to screen coordinates.</li>
     *   <li>Track focus/foreground to distinguish visible vs minimized.</li>
     * </ol>
     */
    fun detect(game: GameType): GameWindowInfo? {
        try {
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val devices = ge.getScreenDevices()
            if (devices.size == 0) return null
            val primary = devices[0]
            val b = primary.getDefaultConfiguration().getBounds()
            val bounds = Rect(b.x, b.y, b.width, b.height)
            val win = GameWindowInfo(0L, bounds, bounds.width, bounds.height)
            win.visible = true
            log.debug("GameWindowDetector", "Demo window on primary display $bounds")
            return win
        } catch (t: Throwable) {
            log.warn("GameWindowDetector", "Screen detection failed: $t")
            return null
        }
    }

    companion object {
        /** Returns the current desktop-absolute mouse position. */
        fun pointerLocation(): Point {
            try {
                val pi = MouseInfo.getPointerInfo()
                val p = pi.getLocation()
                return Point(p.x, p.y)
            } catch (t: Throwable) {
                return Point(0, 0)
            }
        }
    }
}