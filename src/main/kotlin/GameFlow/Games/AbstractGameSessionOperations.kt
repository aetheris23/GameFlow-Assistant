package GameFlow.Games

import GameFlow.Input.MouseController
import GameFlow.Models.GameWindowInfo
import GameFlow.Models.Rect
import GameFlow.Vision.GrayImage
import GameFlow.Vision.MatchResult
import GameFlow.Vision.TemplateMatcher

/**
 * Template-driven click action shared by both supported games. A concrete
 * profile maps a game state to one or more buttons; the session searches each
 * button's ROI for its template and, on a match, clicks the center using
 * window-relative coordinates.
 *
 * <p>If no template matches, {@code clickButton} returns false; the engine's
 * SafetyManager retries within its budget and finally halts. It never clicks
 * blindly on an unconfirmed screen.</p>
 */
class AbstractGameSessionOperations {

    private val DEFAULT_THRESHOLD = 0.6f

    protected val templates: TemplateLibrary
    protected val matcher: TemplateMatcher
    protected val mouse: MouseController
    protected val refW: Int
    protected val refH: Int
    protected val clickCooldownMs: Int

    private var window: GameWindowInfo? = null

    constructor(templates: TemplateLibrary, matcher: TemplateMatcher, mouse: MouseController,
                refW: Int, refH: Int, clickCooldownMs: Int) {
        this.templates = templates
        this.matcher = matcher
        this.mouse = mouse
        this.refW = refW
        this.refH = refH
        this.clickCooldownMs = clickCooldownMs
    }

    fun attachWindow(window: GameWindowInfo) { this.window = window }
    fun window(): GameWindowInfo? = window
    fun templates(): TemplateLibrary = templates
    fun clickCooldown(): Int = clickCooldownMs

    /** Loads a template for the given button id within a game resource dir. */
    protected fun template(gameDir: String, templateFile: String): GrayImage? =
        templates.load(gameDir + "/" + templateFile)

    /**
     * Searches for {@code templateFile} inside the frame at the given reference
     * ROI, scaled to the current window, and clicks its center when matched.
     */
    fun clickButton(frame: GrayImage?, gameDir: String, templateFile: String,
                    refRoi: Rect, win: GameWindowInfo?): Boolean {
        if (frame == null || win == null || !win.valid()) return false
        val tpl = template(gameDir, templateFile)
        if (tpl == null) return false

        val roi = refRoi.scaledTo(win.screenBounds.width, win.screenBounds.height, refW, refH)
        val m = matcher.find(frame, tpl, roi, DEFAULT_THRESHOLD)
        if (!m.found) return false
        return mouse.leftClick(win, m.center!!, clickCooldownMs)
    }

    /** Whether the template is visible anywhere in the current frame's ROI. */
    protected fun visible(frame: GrayImage?, gameDir: String, templateFile: String,
                          refRoi: Rect): Boolean {
        if (frame == null) return false
        val w = window
        if (w == null || !w.valid()) return false
        val tpl = template(gameDir, templateFile)
        if (tpl == null) return false
        val roi = refRoi.scaledTo(w.screenBounds.width, w.screenBounds.height, refW, refH)
        return matcher.find(frame, tpl, roi, DEFAULT_THRESHOLD).found
    }
}