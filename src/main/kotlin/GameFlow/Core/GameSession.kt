package GameFlow.Core

import GameFlow.Vision.GrayImage
import GameFlow.Models.GameState
import GameFlow.Models.GameType
import GameFlow.Models.GameWindowInfo

/**
 * Game-specific automation session (implemented per game in the Games package).
 * The engine drives gameplay purely through this seam, so adding a new game only
 * requires a new implementation. Responsibilities:
 *  - classify the current frame into a {@link GameState}
 *  - detect points where the user must be asked (e.g. a story dialogue choice)
 *  - perform the correct, state-appropriate action (Next/Continue/Skip/Claim...)
 */
interface GameSession : AutoCloseable {

    fun game(): GameType

    /** Returns the live detected window; null/not valid before detection. */
    fun window(): GameWindowInfo?

    /** Hands the detected window to the session for click layout. */
    fun attachWindow(window: GameWindowInfo)

    /** Classifies a frame into a FSM state. Never returns null. */
    fun detectState(frame: GrayImage): GameState

    /** True when the screen currently shows a dialogue choice the user must make. */
    fun isDialogChoice(frame: GrayImage): Boolean

    /**
     * Executes the action appropriate for the current state on the given frame.
     * @return true if the session believes it can keep progressing this tick.
     */
    fun step(state: GameState, frame: GrayImage): Boolean

    /** A short human phrase for the dashboard's "Current Task" field. */
    fun taskHint(state: GameState): String

    override fun close() { }
}