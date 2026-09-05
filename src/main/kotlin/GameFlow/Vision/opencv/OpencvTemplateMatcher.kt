package GameFlow.Vision.opencv

import GameFlow.Models.Point
import GameFlow.Models.Rect
import GameFlow.Vision.GrayImage
import GameFlow.Vision.MatchResult
import GameFlow.Vision.TemplateMatcher

/**
 * REFERENCE SKELETON for a real OpenCV-based {@link TemplateMatcher} using the
 * JavaCV bindings.
 *
 * <p>This file is intentionally NOT part of the default build (see build.gradle)
 * and is only added when the "opencv" profile is enabled. Its purpose is to
 * document the integration seam: mirroring the pure matcher's
 * {@link TemplateMatcher} contract so game profiles never change when the screen
 * engine is swapped.</p>
 *
 * <p>Because the exact JavaCV API surface depends on the pinned version, verify
 * the calls below against your JavaCV version (org.bytedeco:javacv + opencv) and
 * the current template-matching conventions before enabling it. The default
 * PureJavaTemplateMatcher is used otherwise and satisfies the low-resource goal.</p>
 */
class OpencvTemplateMatcher : TemplateMatcher {

    private val available: Boolean

    constructor() {
        this.available = isAvailable()
    }

    private fun isAvailable(): Boolean {
        try {
            Class.forName("org.bytedeco.opencv.opencv_core.CvMat")
            return true
        } catch (t: Throwable) {
            return false
        }
    }

    override fun engineName(): String = "opencv"

    fun supportsTheAPI(): Boolean = available

    override fun find(source: GrayImage, template: GrayImage, roi: Rect?, threshold: Float): MatchResult {
        // Implementation outline (validate against pinned JavaCV version):
        //   1. Convert GrayImage source+template to grayscale CvMat.
        //   2. Optionally crop to the window-relative ROI first.
        //   3. Call CvMatchTemplate(TM_CCOEFF_NORMED).
        //   4. minMaxLoc -> best score + location; accept if score >= threshold.
        //   5. Release native mats via Mat.release() (unmanaged resources).
        //   6. Return MatchResult.hit(score, center, region) mirrored to the ROI's
        //      window-relative coordinates.
        throw UnsupportedOperationException(
            "OpenCV engine skeleton: implement against the pinned JavaCV version " +
                "or rely on the default PureJavaTemplateMatcher.")
    }
}