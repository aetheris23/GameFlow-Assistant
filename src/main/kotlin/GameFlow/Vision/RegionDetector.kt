package GameFlow.Vision

import GameFlow.Models.Rect

/**
 * Locates meaningful screen regions to drive ROI-based processing (bottom button
 * bar, dialog box, mission panel). Scans are restricted to these areas, which is
 * what keeps template matching cheap.
 */
interface RegionDetector {
    fun dialogArea(frame: GrayImage): Rect
    fun buttonBar(frame: GrayImage): Rect
    fun missionPanel(frame: GrayImage): Rect
}