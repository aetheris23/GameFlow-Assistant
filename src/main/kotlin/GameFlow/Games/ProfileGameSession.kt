package GameFlow.Games

import GameFlow.Input.MouseController
import GameFlow.Models.Rect
import GameFlow.Models.GameState
import GameFlow.Models.GameType
import GameFlow.Models.GameWindowInfo
import GameFlow.Vision.GrayImage
import GameFlow.Vision.TemplateMatcher
import GameFlow.Core.GameSession

/**
 * Generic {@link GameSession} driven by a {@link GameProfile}: it classifies the
 * frame via the profile, finds the right {@link ButtonSpec} in the profile's
 * plan and clicks it through the template matcher + mouse. Both games use the
 * same machinery — only their profiles differ.
 */
class ProfileGameSession : GameSession {

    private val profile: GameProfile
    private val ops: AbstractGameSessionOperations
    private var window: GameWindowInfo? = null

    constructor(profile: GameProfile, templates: TemplateLibrary, matcher: TemplateMatcher,
                mouse: MouseController, clickCooldownMs: Int) {
        this.profile = profile
        this.ops = AbstractGameSessionOperations(templates, matcher, mouse,
            profile.referenceWidth(), profile.referenceHeight(), clickCooldownMs)
    }

    override fun game(): GameType = profile.type()
    override fun window(): GameWindowInfo? = window

    /** Attaches the detected game window so clicks use its real bounds. */
    override fun attachWindow(window: GameWindowInfo) {
        this.window = window
        ops.attachWindow(window)
    }

    override fun detectState(frame: GrayImage): GameState = profile.classify(frame, ops.templates())

    override fun isDialogChoice(frame: GrayImage): Boolean = profile.isDialogueChoice(frame, ops.templates())

    override fun step(state: GameState, frame: GrayImage): Boolean {
        val refW = profile.referenceWidth()
        val refH = profile.referenceHeight()
        for (spec in profile.actionsFor(state)) {
            val r = spec.roi
            val refRoi = Rect(
                (r.fx * refW).toInt(), (r.fy * refH).toInt(),
                (r.fw * refW).toInt(), (r.fh * refH).toInt())
            if (ops.clickButton(frame, profile.resourceDir(), spec.template, refRoi, window)) {
                return true
            }
        }
        return false
    }

    override fun taskHint(state: GameState): String = profile.taskHint(state)
}