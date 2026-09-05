package GameFlow.Vision

/**
 * Implements the "capture -> compare -> process only when necessary" strategy.
 * Keeps only one previous frame (bounded memory) to detect screen change; the
 * automation skips heavy processing when the frame is unchanged.
 */
class ChangeDetector {
    private var last: GrayImage? = null

    /** @return true when the frame differs from the last one beyond the threshold. */
    fun hasChanged(frame: GrayImage?, threshold: Float): Boolean {
        if (frame == null) return false
        val prev = last
        if (prev == null) {
            last = frame
            return true
        }
        val same = frame.width == prev.width && frame.height == prev.height &&
                frame.similarity(prev) >= threshold
        last = frame
        return !same
    }

    fun reset() { last = null }
}