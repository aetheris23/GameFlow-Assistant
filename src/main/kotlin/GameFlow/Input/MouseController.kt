package GameFlow.Input

import java.awt.Robot
import java.awt.AWTException
import java.awt.event.InputEvent

import GameFlow.Models.GameWindowInfo
import GameFlow.Models.Point
import GameFlow.Models.Rect

/**
 * Mouse controller. Converts window-relative coordinates to screen coordinates
 * using the detected window bounds, so the same profile works at any window
 * size. All gestures have a small cooldown to keep actions safe and deliberate.
 */
class MouseController : AutoCloseable {

    private var robot: Robot? = null
    private var lastActionTime: Long = 0L

    private fun ensureRobot(): Robot {
        var r = robot
        if (r == null) {
            try {
                r = Robot()
            } catch (e: AWTException) {
                throw RuntimeException("Mouse input unavailable", e)
            }
            robot = r
        }
        return r
    }

    /** Window-relative -> screen (desktop) point. */
    fun toScreen(window: GameWindowInfo, windowPoint: Point): Point {
        val b = window.screenBounds
        return Point(b.x + windowPoint.x, b.y + windowPoint.y)
    }

    /** Moves to a screen point. Returns true if permitted by the cooldown. */
    fun moveTo(window: GameWindowInfo, windowPoint: Point, cooldownMs: Int): Boolean {
        if (!cooled(cooldownMs)) return false
        val s = toScreen(window, windowPoint)
        ensureRobot().mouseMove(s.x, s.y)
        return true
    }

    fun leftClick(window: GameWindowInfo, windowPoint: Point, cooldownMs: Int): Boolean {
        if (!cooled(cooldownMs)) return false
        val s = toScreen(window, windowPoint)
        ensureRobot().mouseMove(s.x, s.y)
        ensureRobot().mousePress(InputEvent.BUTTON1_DOWN_MASK)
        ensureRobot().mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
        return true
    }

    fun clickCenter(window: GameWindowInfo, region: Rect?, cooldownMs: Int): Boolean {
        if (region == null) return false
        return leftClick(window, region.center(), cooldownMs)
    }

    fun msSinceLastAction(): Long = System.currentTimeMillis() - lastActionTime

    private fun cooled(cooldownMs: Int): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < cooldownMs) return false
        lastActionTime = now
        return true
    }

    override fun close() { }
}