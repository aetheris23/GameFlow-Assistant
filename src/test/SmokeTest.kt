package GameFlow

import java.nio.file.Files

import GameFlow.Core.AdaptivePoller
import GameFlow.Core.SafetyManager
import GameFlow.Core.StateMachine
import GameFlow.Core.TaskManager
import GameFlow.Database.DatabaseContext
import GameFlow.Database.Repositories.SettingsRepository
import GameFlow.Models.FeatureType
import GameFlow.Models.GameState
import GameFlow.Models.GameType
import GameFlow.Models.PollMode
import GameFlow.Models.Task
import GameFlow.Models.TaskResult
import GameFlow.Services.LoggingService
import GameFlow.Vision.ChangeDetector
import GameFlow.Vision.GrayImage
import GameFlow.Vision.PureJavaTemplateMatcher

/**
 * Port of the Java smoke test that exercised the core pipeline without native
 * deps or a real window. Run with: kotlinc + run, or `./run.sh --headless` which
 * exercises the engine. This file is a fast pure-JVM functional check.
 */
class SmokeTest {

    fun run(testOut: StringBuilder) {
        val log = LoggingService(500)
        matchTest()
        changeTest()
        fsmTest(log)
        safetyTest(log)
        queueTest(log)
        dbTest()
        pollTest()
        simulatedSessionTest()
        testOut.append("All smoke checks passed.\n")
    }

    private fun matchTest() {
        val ts = 16
        val src = GrayImage(ts * 3, ts * 3)
        val tpl = GrayImage(ts, ts)
        for (y in 0 until src.height) { for (x in 0 until src.width) src.set(x, y, 5f) }
        for (y in 0 until ts) {
            for (x in 0 until ts) {
                val v = (x * 20 + y * 30).toFloat()
                tpl.set(x, y, v)
                src.set(ts + x, ts + y, v)
            }
        }
        val m = PureJavaTemplateMatcher()
        check(m.find(src, tpl, null, 0.6f).found, "matcher finds the embedded textured template")
        check(!m.find(src, GrayImage(ts, ts), null, 0.6f).found, "matcher rejects a flat (empty variance) template")
    }

    private fun changeTest() {
        val cd = ChangeDetector()
        val f = GrayImage(8, 8)
        check(cd.hasChanged(f, 0.98f), "first frame is a change")
        check(!cd.hasChanged(f, 0.98f), "identical frame is not a change")
        val g = GrayImage(8, 8)
        g.set(0, 0, 255f)
        check(cd.hasChanged(g, 0.98f), "mutated frame is a change")
    }

    private fun fsmTest(log: LoggingService) {
        val fsm = StateMachine(log, "smoke")
        check(fsm.get() == GameState.UNKNOWN, "fsm starts UNKNOWN")
        fsm.transitionTo(GameState.LOADING)
        fsm.transitionTo(GameState.STORY)
        check(fsm.get() == GameState.STORY, "fsm reached STORY")
        check(fsm.actionable(), "actionable in STORY")
        fsm.transitionTo(GameState.UNKNOWN)
        check(fsm.isUnknown(), "unknown flagged")
    }

    private fun safetyTest(log: LoggingService) {
        val safety = SafetyManager(log, "smoke", 3, 1000, 5)
        safety.start()
        check(safety.beginAction("x"), "action allowed while running")
        check(!safety.recordFailure("x"), "below retry budget")
        check(!safety.recordFailure("x"), "still below retry budget")
        check(safety.recordFailure("x"), "retries exhausted")
    }

    private fun queueTest(log: LoggingService) {
        val tm = TaskManager(log, "smoke", null, GameType.UMA_MUSUME)
        tm.plan(
            Task("s1", "Open mission", FeatureType.STORY, GameState.MISSION),
            Task("s2", "Continue story", FeatureType.STORY, GameState.STORY))
        val t = tm.next()
        check(t != null, "first task popped")
        check(!tm.isEmpty() && tm.completed() == 0, "queue not empty after pop")
        tm.complete(t!!, TaskResult.SUCCESS)
        check(tm.completed() == 1, "one task completed")
    }

    private fun dbTest() {
        val dir = Files.createTempDirectory("gameflow-smoke")
        val db = DatabaseContext(dir.resolve("smoke.db")).open()
        val repo = SettingsRepository(db.connection())
        repo.ensureGameRows()
        val s = repo.load("UmaMusume")
        check(s.pollLoadingMs == 1000, "defaults available from SQLite")
        db.close()
    }

    private fun pollTest() {
        check(AdaptivePoller.modeFor(true, false) == PollMode.PAUSED, "paused maps to PAUSED")
        check(AdaptivePoller.modeFor(false, true) == PollMode.LOADING, "loading maps to LOADING")
        check(AdaptivePoller.modeFor(false, false) == PollMode.ACTIVE, "active maps to ACTIVE")
    }

    private fun simulatedSessionTest() {
        val sim = GameFlow.Games.SimulatedGameSession(GameType.UMA_MUSUME)
        val f = GrayImage(1, 1)
        check(sim.detectState(f) == GameState.LOADING, "simulated session starts LOADING")
    }

    private fun check(cond: Boolean, msg: String) {
        if (!cond) throw AssertionError("FAILED: " + msg)
        println("  ok - " + msg)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val out = StringBuilder()
            try {
                SmokeTest().run(out)
                print(out)
                println("SMOKE TEST PASSED")
            } catch (t: Throwable) {
                System.err.println(t.toString())
                t.printStackTrace()
                System.exit(1)
            }
        }
    }
}
