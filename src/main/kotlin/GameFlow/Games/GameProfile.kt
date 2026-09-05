package GameFlow.Games

import GameFlow.Models.GameState
import GameFlow.Models.GameType
import GameFlow.Vision.GrayImage

/**
 * Per-game configuration + logic that the generic {@link ProfileGameSession}
 * uses to drive automation. Adding a new game (or a new screen for an existing
 * one) means adding a profile, not touching the engine.
 */
interface GameProfile {

    fun type(): GameType

    /** Resource folder under src/main/resources that holds this game's PNGs. */
    fun resourceDir(): String

    fun referenceWidth(): Int
    fun referenceHeight(): Int

    /**
     * Classifies a window frame into the FSM state using template matching
     * against this game's screen templates. Never returns null.
     */
    fun classify(frame: GrayImage, templates: TemplateLibrary): GameState

    /** True when the frame shows a story dialogue choice the user must pick. */
    fun isDialogueChoice(frame: GrayImage, templates: TemplateLibrary): Boolean

    /** The button(s) this game can press for a given state. */
    fun actionsFor(state: GameState): List<ButtonSpec>

    /** Human "current task" phrase for the dashboard, per state. */
    fun taskHint(state: GameState): String
}