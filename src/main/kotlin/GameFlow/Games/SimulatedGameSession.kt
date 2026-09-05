package GameFlow.Games

import GameFlow.Core.GameSession
import GameFlow.Models.GameState
import GameFlow.Models.GameType
import GameFlow.Models.GameWindowInfo
import GameFlow.Vision.GrayImage

/**
 * Deterministic session used for smoke tests and {@code --demo} mode. It cycles
 * a synthetic story sequence (LOADING -> STORY -> DIALOG -> pause on a choice ->
 * etc.) and logs each transition, exercising the whole engine pipeline without a
 * running game. It never touches the real mouse or a real window.
 */
class SimulatedGameSession(private val selectedGame: GameType) : GameSession {

    private var attached: GameWindowInfo? = null
    private val sequence: List<GameState> = listOf(GameState.LOADING, GameState.STORY, GameState.DIALOG, GameState.STORY, GameState.REWARD)

    private var it: Iterator<GameState> = sequence.iterator()
    private var current: GameState? = null
    private var choiceTick: Int = 0

    override fun game(): GameType = selectedGame
    override fun window(): GameWindowInfo? = attached
    override fun attachWindow(window: GameWindowInfo) { this.attached = window }

    override fun detectState(frame: GrayImage): GameState {
        if (current != null && current == GameState.REWARD) return GameState.REWARD
        if (!it.hasNext()) { it = sequence.iterator() }
        current = it.next()
        return current!!
    }

    override fun isDialogChoice(frame: GrayImage): Boolean {
        // Pause once when hitting DIALOG so the demo shows the user-choice pause.
        if (current == GameState.DIALOG) { choiceTick++; return choiceTick == 1 }
        return false
    }

    override fun step(state: GameState, frame: GrayImage): Boolean {
        // In simulation every action "succeeds" so the queue advances and logs
        // clearly; real sessions return false when a template is not found.
        return true
    }

    override fun taskHint(s: GameState): String {
        return if (s == GameState.STORY) "Advancing story"
                else if (s == GameState.DIALOG) "Waiting for dialog / next button"
                else if (s == GameState.REWARD) "Claiming reward"
                else if (s == GameState.LOADING) "Loading..."
                else "Idle / Unknown"
    }
}