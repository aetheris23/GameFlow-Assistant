package GameFlow.Vision

import GameFlow.Models.Rect

/**
 * Performs template matching on a game frame. Implementations are expected to
 * operate on a Region Of Interest (ROI) so only the relevant part of the screen
 * is scanned, which keeps CPU cost low.
 */
interface TemplateMatcher {

    /**
     * Searches for a template inside {@code source}.
     *
     * @param source    full game frame (already cropped to the window)
     * @param template  resource template, any size
     * @param roi       region of interest, or null to scan the whole frame
     * @param threshold minimum normalized score in [0,1] to accept a match
     */
    fun find(source: GrayImage, template: GrayImage, roi: Rect?, threshold: Float): MatchResult

    /** Name shown in logs so users can tell the OpenCV path from the fallback. */
    fun engineName(): String
}