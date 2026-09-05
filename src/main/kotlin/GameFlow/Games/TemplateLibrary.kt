package GameFlow.Games

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap

import GameFlow.Models.Rect
import GameFlow.Vision.GrayImage
import GameFlow.Vision.TemplateMatcher
import GameFlow.Vision.TemplateMatchers

/**
 * Small loader for game window templates (PNG) shipped under
 * src/main/resources/&lt;Game&gt;/. Images load lazily and are cached; a missing
 * template yields null so callers stay safe instead of crashing.
 */
class TemplateLibrary {

    private val cache: ConcurrentHashMap<String, GrayImage> = ConcurrentHashMap()
    private val matcher: TemplateMatcher = TemplateMatchers.createDefault()

    /** @param resourcePath e.g. "UmaMusume/next.png" (no leading slash) */
    fun load(resourcePath: String): GrayImage? {
        if (cache.containsKey(resourcePath)) return cache[resourcePath]
        val img = read(resourcePath)
        if (img != null) cache.put(resourcePath, img)
        return img
    }

    /**
     * True when the template is found within the optional ROI at a safe
     * threshold. Missing templates or frames simply report false.
     */
    fun safeMatch(frame: GrayImage?, resourcePath: String, roi: Rect?): Boolean {
        if (frame == null) return false
        val tpl = load(resourcePath)
        if (tpl == null) return false
        return matcher.find(frame, tpl, roi, 0.60f).found
    }

    private fun read(path: String): GrayImage? {
        try {
            val url = (TemplateLibrary::class).java.getResource("/" + path)
            if (url == null) return null
            val img = ImageIO.read(url)
            if (img == null) return null
            val w = img.getWidth(); val h = img.getHeight()
            val g = GrayImage(w, h)
            var y = 0
            while (y < h) {
                var x = 0
                while (x < w) {
                    val argb = img.getRGB(x, y)
                    val r = argb.shr(16).and(0xFF)
                    val gr = argb.shr(8).and(0xFF)
                    val b = argb.and(0xFF)
                    g.set(x, y, 0.299f * r + 0.587f * gr + 0.114f * b)
                    x++
                }
                y++
            }
            return g
        } catch (t: Throwable) {
            return null
        }
    }

    fun cached(): Int = cache.size
}