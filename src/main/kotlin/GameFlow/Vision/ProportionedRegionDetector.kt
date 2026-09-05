package GameFlow.Vision

import GameFlow.Models.Rect

/**
 * Baseline ROIs for the common layout: dialog text occupies the lower-middle,
 * action buttons concentrate near the bottom, and the mission panel is a large
 * upper block. Regions are authored in fractional coordinates and scaled to the
 * current window size at runtime.
 */
class ProportionedRegionDetector(private val refW: Int, private val refH: Int) : RegionDetector {

    override fun dialogArea(frame: GrayImage): Rect = scale(frame, 0.40f, 0.20f, 0.60f, 0.40f)

    override fun buttonBar(frame: GrayImage): Rect = scale(frame, 0.50f, 0.80f, 0.48f, 0.18f)

    override fun missionPanel(frame: GrayImage): Rect = scale(frame, 0.10f, 0.10f, 0.80f, 0.45f)

    private fun scale(f: GrayImage, fx: Float, fy: Float, fw: Float, fh: Float): Rect =
        Rect(
            Math.round(fx * refW),
            Math.round(fy * refH),
            Math.round(fw * refW),
            Math.round(fh * refH))
            .scaledTo(f.width, f.height, refW, refH)
}