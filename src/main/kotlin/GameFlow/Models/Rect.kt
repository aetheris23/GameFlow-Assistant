package GameFlow.Models

/**
 * Rectangle with window-relative coordinates. When authored in reference
 * resolution the origin is the top-left of the detected window; scale with
 * {@link #scaledTo} so input stays correct after a resize.
 */
class Rect(val x: Int, val y: Int, val width: Int, val height: Int) {

    fun right(): Int = x + width
    fun bottom(): Int = y + height

    /** Scales this rect (authored at refW x refH) to a new window size. */
    fun scaledTo(newWidth: Int, newHeight: Int, refWidth: Int, refHeight: Int): Rect {
        val sx = newWidth.toFloat() / refWidth
        val sy = newHeight.toFloat() / refHeight
        return Rect(
            Math.round(x * sx),
            Math.round(y * sy),
            Math.round(width * sx),
            Math.round(height * sy))
    }

    fun center(): Point = Point(x + width / 2, y + height / 2)

    override fun toString(): String = "[$x,$y $width x $height]"
}