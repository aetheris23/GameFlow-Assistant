package GameFlow.Models

/** Immutable integer point in window-relative coordinates. */
class Point(val x: Int, val y: Int) {
    override fun toString(): String = "($x,$y)"
}