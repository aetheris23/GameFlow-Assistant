package GameFlow.Games

import GameFlow.Models.GameState

/**
 * A button a game session may need to press. The ROI is authored in fractional
 * (0..1) coordinates relative to the reference window, so it scales with the
 * current window size. {@code template} is a PNG shipped in the game's resource
 * dir and {@code threshold} is the minimum normalized match score.
 */
class ButtonSpec(val state: GameState, val template: String, val roi: RectF, val label: String) {

    /** Fractional (x=left .. 1, w=width fraction) reference ROI. */
    class RectF(val fx: Float, val fy: Float, val fw: Float, val fh: Float) {
        companion object {
            fun of(fx: Float, fy: Float, fw: Float, fh: Float): RectF = RectF(fx, fy, fw, fh)
        }
    }

    companion object {
        fun of(state: GameState, template: String, roi: RectF, label: String): ButtonSpec =
            ButtonSpec(state, template, roi, label)
    }
}