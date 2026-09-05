package GameFlow.Models

/**
 * Snapshot of a detected game window. Holds both the native OS window handle
 * and the on-screen bounds; the engine derives window-relative coordinates from
 * these bounds so resize/DPI changes are handled.
 */
class GameWindowInfo(
    /** Native OS window id (Windows: HWND; handled by the detector). */
    val nativeHandle: Long,
    /** Screen-absolute bounds of the window content area. */
    val screenBounds: Rect,
    /** Reference resolution the game's templates were authored at. */
    val referenceWidth: Int,
    val referenceHeight: Int) {

    /** Whether the window currently exists and is usable. */
    var visible: Boolean = false

    fun valid(): Boolean =
        visible && screenBounds.width > 0 && screenBounds.height > 0

    override fun toString(): String =
        "window#$nativeHandle @ " + (if (screenBounds != null) screenBounds else "?") +
            (if (valid()) " visible" else " hidden")
}