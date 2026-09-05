package GameFlow.Vision

import GameFlow.Models.Point
import GameFlow.Models.Rect

/** Outcome of searching for a template inside a frame. */
class MatchResult private constructor(
    val found: Boolean,
    val score: Float,      // 0..1 normalized correlation
    val center: Point?,    // window-relative (may be null when not found)
    val region: Rect?) {   // window-relative matched area (may be null)

    companion object {
        fun hit(score: Float, center: Point, region: Rect): MatchResult =
            MatchResult(true, score, center, region)

        fun miss(): MatchResult = MatchResult(false, 0f, null, null)
    }
}