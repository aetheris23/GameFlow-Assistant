package GameFlow.Input

import java.awt.Robot
import java.awt.AWTException
import java.awt.event.KeyEvent

/**
 * Keyboard controller (AWT Robot). Used for shortcuts/hotkeys such as
 * escape, skip, or a user-configured advance key. Each key tap respects the
 * engine cooldown to avoid input bursts.
 */
class KeyboardController : AutoCloseable {

    private var robot: Robot? = null

    fun tapKey(keyCode: Int, delayAfterMs: Int) {
        ensureRobot().keyPress(keyCode)
        ensureRobot().keyRelease(keyCode)
        sleep(delayAfterMs)
    }

    /** Escape is the safe "back/confirm" fallback across many screens. */
    fun pressEscape(delayAfterMs: Int) { tapKey(KeyEvent.VK_ESCAPE, delayAfterMs) }

    fun typeEasily(text: String) {
        // Placeholder for OCR-verified text entry; reserved for dialog keeper.
        throw UnsupportedOperationException("not configured")
    }

    private fun ensureRobot(): Robot {
        var r = robot
        if (r == null) {
            try {
                r = Robot()
            } catch (e: AWTException) {
                throw RuntimeException("Keyboard input unavailable", e)
            }
            robot = r
        }
        return r
    }

    private fun sleep(ms: Int) {
        try { Thread.sleep(Math.max(0, ms).toLong()) } catch (ignored: InterruptedException) { Thread.currentThread().interrupt() }
    }

    override fun close() { }
}