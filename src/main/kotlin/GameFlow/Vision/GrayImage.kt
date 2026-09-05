package GameFlow.Vision

import java.awt.Rectangle
import GameFlow.Models.Rect

/**
 * Lightweight grayscale frame (0..255 float per pixel, row-major) used by the
 * whole vision pipeline. Buffers are small and reused where practical to keep
 * memory usage low on weak hardware.
 */
class GrayImage(val width: Int, val height: Int) {
    val data: FloatArray = FloatArray(width * height)

    fun at(x: Int, y: Int): Float = data[y * width + x]
    fun set(x: Int, y: Int, v: Float) { data[y * width + x] = v }

    fun asRect(): Rectangle = Rectangle(0, 0, width, height)

    /** Simple average intensity of a region as a cheap screen-change signal. */
    fun regionMean(r: Rect): Float {
        var sum = 0.0
        var count = 0
        val x0 = Math.max(0, r.x); val y0 = Math.max(0, r.y)
        val x1 = Math.min(width, r.right())
        val y1 = Math.min(height, r.bottom())
        var y = y0
        while (y < y1) {
            val row = y * width
            var x = x0
            while (x < x1) { sum += data[row + x]; count++; x++ }
            y++
        }
        return if (count == 0) 0f else (sum / count).toFloat()
    }

    /**
     * Similarity in [0,1] vs another same-size frame (1 = identical).
     * Backs the "capture and process only when the screen changed" optimization.
     */
    fun similarity(other: GrayImage?): Float {
        if (other == null || other.width != width || other.height != height) return 0f
        var acc = 0.0
        for (i in 0 until data.size) {
            val d = data[i] - other.data[i]
            acc += d * d
        }
        val mse = acc / data.size
        return Math.max(0.0, 1.0 - Math.sqrt(mse) / 128.0).toFloat()
    }

    /** Down-bins the image by an integer factor for fast coarse matching. */
    fun downsample(factor: Int): GrayImage {
        if (factor <= 1) return this
        val w = width / factor
        val h = height / factor
        val out = GrayImage(w, h)
        var y = 0
        while (y < h) {
            val destRow = y * w
            var x = 0
            while (x < w) {
                var s = 0f
                var dy = 0
                while (dy < factor) {
                    val srcRow = (y * factor + dy) * width
                    var dx = 0
                    while (dx < factor) { s += data[srcRow + x * factor + dx]; dx++ }
                    dy++
                }
                out.data[destRow + x] = s / (factor * factor)
                x++
            }
            y++
        }
        return out
    }

    companion object {
        fun gray(pixels: Array<Array<Float>>): GrayImage {
            val h = pixels.size
            val w = pixels[0].size
            val out = GrayImage(w, h)
            for (y in 0 until h) System.arraycopy(pixels[y], 0, out.data, y * w, w)
            return out
        }
    }
}