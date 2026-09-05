package GameFlow.Games.UmaMusume

import GameFlow.Models.GameState
import GameFlow.Models.GameType
import GameFlow.Games.ButtonSpec
import GameFlow.Games.GameProfile
import GameFlow.Games.TemplateLibrary
import GameFlow.Vision.GrayImage

/**
 * Uma Musume profile: reference resolution, per-state buttons, classifier and
 * task hints. The buttons below are the intended convention (see README for how
 * to drop in real PNG templates). If a template is missing the session safely
 * returns "no action" rather than clicking randomly.
 */
class UmaMusumeProfile : GameProfile {

    companion object {
        val DIR = "UmaMusume"
        val REF_W = 1920
        val REF_H = 1080
    }

    override fun type(): GameType = GameType.UMA_MUSUME
    override fun resourceDir(): String = DIR
    override fun referenceWidth(): Int = REF_W
    override fun referenceHeight(): Int = REF_H

    /**
     * Template-driven screen classifier. Each state is confirmed by searching a
     * small identifying template within its typical ROI; frames that match none
     * of them are reported as UNKNOWN (which pauses, never clicks).
     */
    override fun classify(frame: GrayImage, t: TemplateLibrary): GameState {
        if (t.safeMatch(frame, "$DIR/loading.png", null)) return GameState.LOADING
        if (t.safeMatch(frame, "$DIR/main_menu.png", null)) return GameState.MAIN_MENU
        if (t.safeMatch(frame, "$DIR/story_main.png", null)) return GameState.STORY
        if (t.safeMatch(frame, "$DIR/mission.png", null)) return GameState.MISSION
        return GameState.UNKNOWN
    }

    override fun isDialogueChoice(frame: GrayImage, t: TemplateLibrary): Boolean =
        t.safeMatch(frame, "$DIR/dialog_choice.png", null)

    override fun actionsFor(state: GameState): List<ButtonSpec> {
        return if (state == GameState.STORY || state == GameState.DIALOG)
            listOf(
                ButtonSpec.of(state, "next.png", ButtonSpec.RectF.of(0.55f, 0.80f, 0.20f, 0.10f), "Next"),
                ButtonSpec.of(state, "continue.png", ButtonSpec.RectF.of(0.72f, 0.80f, 0.20f, 0.10f), "Continue"))
        else if (state == GameState.CONFIRMATION)
            listOf(ButtonSpec.of(state, "confirm.png", ButtonSpec.RectF.of(0.55f, 0.82f, 0.20f, 0.10f), "Confirm"))
        else if (state == GameState.REWARD)
            listOf(ButtonSpec.of(state, "claim.png", ButtonSpec.RectF.of(0.55f, 0.82f, 0.20f, 0.10f), "Claim"))
        else if (state == GameState.MISSION)
            listOf(ButtonSpec.of(state, "start_mission.png", ButtonSpec.RectF.of(0.55f, 0.82f, 0.20f, 0.10f), "Start mission"))
        else emptyList()
    }    override fun taskHint(s: GameState): String {
        return if (s == GameState.STORY) "Advancing story"
                else if (s == GameState.DIALOG) "Waiting for dialog / next button"
                else if (s == GameState.CONFIRMATION) "Confirming"
                else if (s == GameState.REWARD) "Claiming reward"
                else if (s == GameState.MISSION) "Handling mission"
                else if (s == GameState.LOADING) "Loading..."
                else "Idle / Unknown"
    }
}