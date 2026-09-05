package GameFlow.Models

/**
 * FSM states describing the current game screen. The automation engine must
 * always know the current state before performing any action. An UNKNOWN state
 * halts the automation rather than risking a blind click.
 */
enum class GameState {
    UNKNOWN,      // cannot classify -> pause, do not click randomly
    MAIN_MENU,
    STORY,
    DIALOG,
    BATTLE,
    QUEST,
    MISSION,
    REWARD,
    LOADING,
    CONFIRMATION,
    ERROR,
    PAUSED,
    STOPPED
}