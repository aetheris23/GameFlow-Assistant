package GameFlow.Core

import kotlin.jvm.Volatile
import GameFlow.Models.AutomationStatus
import GameFlow.Models.DashboardModel
import GameFlow.Models.GameState
import GameFlow.Models.GameWindowInfo
import GameFlow.Models.PollMode
import GameFlow.Services.LoggingService
import GameFlow.Vision.ChangeDetector
import GameFlow.Vision.GrayImage
import GameFlow.Vision.ScreenCapture
import GameFlow.Vision.TemplateMatcher
import GameFlow.Vision.TemplateMatchers

/**
 * Central automation orchestrator (spec #3/#13/#17). Runs on its own worker
 * thread so the UI thread stays fully responsive. Loop shape:
 *
 *   detect window -> capture -> compare -> (changed?) -> classify state ->
 *   consult SafetyManager -> session.step(action) -> verify -> adapt poll ->
 *   sleep -> repeat
 *
 * The engine never clicks on an UNKNOWN screen; it pauses and asks the user.
 * Pause/stop are driven through the thread-safe control surface below.
 */
class AutomationEngine(
    private val game: GameSession,
    private val fsm: StateMachine,
    private val safety: SafetyManager,
    private val poller: AdaptivePoller,
    private val dash: DashboardModel,
    private val log: LoggingService,
    private val source: String) : AutoCloseable {

    private val capture = ScreenCapture()
    private val changeDetector = ChangeDetector()
    private val matcher: TemplateMatcher = TemplateMatchers.createDefault()

    @Suppress("kotlin:S1135") private val gate: Object = Object()
    @Volatile private var userPaused: Boolean = false
    @Volatile private var halt: Boolean = false
    @Volatile private var worker: Thread? = null

    /** Launches the background worker (first call only). */
    fun start() {
        synchronized (this) {
            val w = worker
            if (w != null && w.isAlive()) return
            halt = false
            val t = Thread(this::run, "gameflow-engine")
            t.setDaemon(true)
            worker = t
            t.start()
        }
        log.info(source, "Automation engine started. CV engine: " + matcher.engineName())
    }

    private fun run() {
        safety.start()
        dash.setAutomation(AutomationStatus.RUNNING)
        try {
            while (!halt && !safety.shouldHalt()) {
                tick()
                awaitGate()
            }
        } catch (t: Throwable) {
            safety.stop("engine loop crashed")
            dash.setErrorStatus(t.toString())
            log.error(source, "Engine crash: $t")
        } finally {
            dash.setAutomation(AutomationStatus.IDLE)
            log.info(source, "Automation loop finished.")
        }
    }

    private fun tick() {
        val win = game.window()
        if (win == null || !win.valid()) {
            dash.setGameStatus("Game is not running. Please start the selected game.")
            log.warn(source, "Waiting for game window...")
            poller.sleepFor(PollMode.LOADING)
            return
        }
        dash.setGameStatus("Running")

        val frame = capture.capture(
            win.screenBounds.x, win.screenBounds.y, win.screenBounds.width, win.screenBounds.height)
        if (frame == null) {
            poller.sleepFor(PollMode.IDLE)
            return
        }

        if (!changeDetector.hasChanged(frame, 0.985f)) {
            poller.sleepFor(PollMode.ACTIVE)
            return
        }

        dispatch(frame)
    }

    /** Classifies and acts on a changed frame. */
    private fun dispatch(frame: GrayImage) {
        val state = game.detectState(frame)
        fsm.transitionTo(state)
        dash.setCurrentState(state.name)
        dash.setCurrentTask(game.taskHint(state))

        if (state == GameState.UNKNOWN) {
            if (safety.isRunning()) {
                safety.pause("Unknown screen detected. Automation has been paused.")
                dash.setErrorStatus("Unknown screen detected. Automation has been paused.")
            }
            poller.sleepFor(PollMode.LOADING)
        } else if (state == GameState.LOADING) {
            dash.setCurrentTask("Loading...")
            poller.sleepFor(PollMode.LOADING)
        } else {
            if (game.isDialogChoice(frame)) {
                notifyDialogChoice()
            } else if (safety.beginAction(state.name)) {
                executeAction(state, frame)
            }
        }
    }

    private fun executeAction(state: GameState, frame: GrayImage) {
        val ok = game.step(state, frame)
        if (ok) {
            safety.recordSuccess(state.name)
        } else if (safety.recordFailure(state.name)) {
            halt()
            return
        }
        safety.endAction()
        safety.cooldown()
        poller.sleepFor(PollMode.ACTIVE)
    }

    /** A dialogue choice freezes the run so the user can pick an option. */
    private fun notifyDialogChoice() {
        safety.pause("Dialogue choice detected. Please select an option.")
        dash.setErrorStatus("Dialogue choice detected. Please select an option.")
        blockUntilResumed()
    }

    private fun blockUntilResumed() {
        synchronized (gate) {
            while (userPaused && !halt) {
                try { gate.wait() } catch (e: InterruptedException) { Thread.currentThread().interrupt() }
            }
        }
    }

    private fun awaitGate() {
        synchronized (gate) {
            while (userPaused && !halt) {
                try { gate.wait(1000) } catch (e: InterruptedException) { Thread.currentThread().interrupt() }
            }
        }
    }

    // ---- UI control surface (thread-safe) ----

    fun pause() {
        synchronized (gate) { if (!halt) userPaused = true }
        safety.pause("user requested")
        dash.setAutomation(AutomationStatus.PAUSED)
        log.info(source, "Pause requested by user.")
    }

    fun resume() {
        synchronized (gate) {
            userPaused = false
            gate.notifyAll()
        }
        safety.resume()
        dash.setAutomation(AutomationStatus.RUNNING)
        dash.setErrorStatus("")
        log.info(source, "Resume requested by user.")
    }

    fun stop() {
        synchronized (gate) {
            halt = true
            userPaused = false
            gate.notifyAll()
        }
        safety.stop("user requested")
        dash.setAutomation(AutomationStatus.STOPPED)
        changeDetector.reset()
        log.info(source, "Stop requested by user.")
    }

    private fun halt() {
        synchronized (gate) {
            halt = true
            userPaused = false
            gate.notifyAll()
        }
        safety.stop("internal safety")
        dash.setAutomation(AutomationStatus.STOPPED)
    }

    fun isUserPaused(): Boolean = userPaused
    fun requestShutdown() { halt() }

    fun fsm(): StateMachine = fsm
    fun safety(): SafetyManager = safety
    fun matcher(): TemplateMatcher = matcher
    fun dashModel(): DashboardModel = dash

    override fun close() {
        requestShutdown()
        capture.close()
    }
}