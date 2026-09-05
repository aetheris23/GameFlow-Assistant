package GameFlow.Vision

import java.awt.Rectangle
import java.awt.Robot
import java.awt.AWTException
import java.awt.image.BufferedImage

/**
 * Screen capture service using AWT Robot. Captures only the on-screen region
 * occupied by the detected game window and converts it to a grayscale frame.
 * The Robot is created lazily so an idle app allocates nothing.
 */
class ScreenCapture : AutoCloseable {

    private var robot: Robot? = null

    private fun ensureRobot(): Robot {
        var r = robot
        if (r == null) {
            try {
                r = Robot()
            } catch (e: AWTException) {
                throw RuntimeException("Screen capture unavailable", e)
            }
            robot = r
        }
        return r
    }

    /**
     * @param screenX, screenY  top-left in screen (desktop) coordinates
     * @return grayscale frame of the requested region, or null if invalid/below 1px
     */
    fun capture(screenX: Int, screenY: Int, width: Int, height: Int): GrayImage? {
        if (width <= 0 || height <= 0) return null
        try {
            val shot = ensureRobot().createScreenCapture(Rectangle(screenX, screenY, width, height))
            return toGray(shot)
        } catch (t: Throwable) {
            // Headless environment or capture failure; treated as "no change".
            return null
        }
    }

    private fun toGray(img: BufferedImage): GrayImage {
        val w = img.getWidth()
        val h = img.getHeight()
        val g = GrayImage(w, h)
        val px: IntArray = img.getRGB(0, 0, w, h, null, 0, w)
        var y = 0
        while (y < h) {
            val row = y * w
            var x = 0
            while (x < w) {
                val argb = px[row + x]
                val r = argb.shr(16).and(0xFF)
                val gr = argb.shr(8).and(0xFF)
                val b = argb.and(0xFF)
                g.set(x, y, 0.299f * r + 0.587f * gr + 0.114f * b)
                x++
            }
            y++
        }
        return g
    }

    override fun close() {
        // Robot holds no JDK resources requiring release; retained for symmetry
        // with AutoCloseable resource management.
    }
}