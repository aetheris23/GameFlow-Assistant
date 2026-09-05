package GameFlow.Vision

import GameFlow.Models.Point
import GameFlow.Models.Rect

/**
 * Zero-dependency template matcher based on normalized cross-correlation on a
 * region of interest. This is the default engine: it requires no native
 * binaries, starts instantly, and has a tiny memory footprint, which matches
 * the "low resource usage" requirement. The OpenCV adapter in OpencvTemplateMatcher
 * is used instead when JavaCV is present on the classpath.
 */
class PureJavaTemplateMatcher : TemplateMatcher {

    private val scaleDown: Int

    constructor() {
        // Down-sample 2x for coarse matching => ~4x fewer operations.
        this.scaleDown = 2
    }

    override fun engineName(): String = "pure-java"

    override fun find(source: GrayImage, template: GrayImage, roi: Rect?, threshold: Float): MatchResult {
        if (source == null || template == null) return MatchResult.miss()
        if (template.width > source.width || template.height > source.height) return MatchResult.miss()

        // Down-sample for fast coarse matching (branch in downsampled space).
        val sw = Math.min(scaleDown, source.width / template.width)
        val sh = Math.min(scaleDown, source.height / template.height)
        val sf = Math.max(1, Math.min(sw, sh))
        val small = if (sf > 1) source.downsample(sf) else source
        val tpl = if (sf > 1) template.downsample(sf) else template

        val dsX0 = if (roi != null) Math.max(0, roi.x / sf) else 0
        val dsY0 = if (roi != null) Math.max(0, roi.y / sf) else 0
        val stepX = 2; val stepY = 2      // sampling steps inside the window

        var best = -1f
        var bestX = 0; var bestY = 0

        val xMax = Math.max(dsX0, small.width - tpl.width)
        val yMax = Math.max(dsY0, small.height - tpl.height)
        var y = dsY0
        while (y <= yMax) {
            var x = dsX0
            while (x <= xMax) {
                val score = scoreAt(small, tpl, x, y, stepX, stepY)
                if (score > best) {
                    best = score
                    bestX = x
                    bestY = y
                }
                x++
            }
            y++
        }
        if (best < threshold) return MatchResult.miss()

        // Map back to source (original resolution) coordinates.
        val ox = bestX * sf
        val oy = bestY * sf
        val cx = ox + template.width / 2
        val cy = oy + template.height / 2
        return MatchResult.hit(best, Point(cx, cy),
            Rect(ox, oy, template.width, template.height))
    }

    private fun scoreAt(src: GrayImage, tpl: GrayImage, ox: Int, oy: Int,
                        stepX: Int, stepY: Int): Float {
        var sxy = 0.0; var sx = 0.0; var sy = 0.0; var sxx = 0.0; var syy = 0.0
        var n = 0
        var ty = 0
        while (ty < tpl.height) {
            val sy0 = (oy + ty) * src.width
            val ty0 = ty * tpl.width
            var tx = 0
            while (tx < tpl.width) {
                val a = src.data[sy0 + ox + tx].toDouble()
                val b = tpl.data[ty0 + tx].toDouble()
                sxy += a * b; sx += a; sy += b; sxx += a * a; syy += b * b
                n++
                tx += stepX
            }
            ty += stepY
        }
        if (n == 0) return 0f
        val num = n * sxy - sx * sy
        val den = Math.sqrt(Math.max(0.0, (n * sxx - sx * sx)) * Math.max(0.0, (n * syy - sy * sy)))
        if (den <= 1e-9) return 0f
        return (num / den).toFloat()
    }
}