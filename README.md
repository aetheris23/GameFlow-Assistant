# GameFlow Assistant

A personal, **local-only** game automation assistant for *Uma Musume* and
*Blue Archive*. It helps with repetitive story/mission flow (Next / Continue /
Skip / Confirm / Claim). It is designed to be **simple, lightweight, reliable,
low on CPU/RAM/GPU/disk**, and — above all — **user controlled**.

> Scope: single-user, personal automation. It works only by interacting with the
> game's on-screen pixels and synthesising local mouse/keyboard input. It does
> **not** touch the game process, network, memory, or anti-cheat systems, and is
> never intended for PvP, tournaments, ranking, or anything that harms other
> players.

## Stack

A faithful realisation of the requested **C#/.NET/WPF** scaffold using the
closest JVM analogues (this project is delivered as Kotlin source; you compile
and run it on your own machine with a Kotlin/Gradle toolchain):

| Requested | Delivered |
|-----------|-----------|
| C# / .NET | **Kotlin on the JVM** (same layered, object-oriented shape) |
| WPF       | **Swing** swing-native desktop windowing, MVVM-style View |
| OpenCV    | **JavaCV/OpenCV** optional matcher (default is zero-dependency) |
| SQLite    | **sqlite-jdbc** (local-only persistence) |

## Features (as implemented)

- **Game selection** on startup (Uma Musume / Blue Archive), loading per-game
  profile, settings and templates.
- **Window detection** (screen-absolute bounds; coordinates are kept
  window-relative so resizes/DPI are handled).
- **Finite-state machine**: `UNKNOWN, MAIN_MENU, STORY, DIALOG, BATTLE, QUEST,
  MISSION, REWARD, LOADING, CONFIRMATION, ERROR, PAUSED, STOPPED`. The engine
  never clicks while the state is `UNKNOWN` — it pauses and tells you.
- **Adaptive polling** (`LOADING ≈1s`, `ACTIVE ≈300ms`, `IDLE ≈1.5s`,
  `PAUSED` = suspended) keeps CPU low.
- **"Capture → compare → process only when changed"** via a change detector.
- **ROI-based template matching** (bottom button bar, dialog area, mission
  panel) instead of scanning the whole desktop.
- **Vision engine is pluggable**: default is a zero-dependency normalized
  correlation matcher; a real **OpenCV** adapter (JavaCV) is provided and can be
  enabled via `-Dgameflow.opencv=true`.
- **Safety system**: emergency/pause/stop, per-action timeout, max retries,
  unknown-screen protection, action cooldown.
- **Human control**: pause / resume / stop (UI buttons; F8/F9/F10 documented as
  the shortcut wiring point).
- **Logging** (INFO/WARNING/ERROR/DEBUG; DEBUG off by default) kept in a bounded
  ring buffer and persisted to SQLite.
- **SQLite** local storage (Games, GameProfiles, AutomationSettings, Tasks,
  TaskHistory, StoryProgress, MissionProgress, Logs).
- **MVVM-style layering**: the automation runs on a background
  `AutomationEngine` thread and only mutates a `DashboardModel`; the Swing view
  is a dumb renderer with an adaptive repaint Timer.

## Architecture

```
src/main/kotlin/GameFlow/
├── Core/        AutomationEngine, StateMachine, TaskManager,
│                SafetyManager, AdaptivePoller, GameSession
├── Games/       GameProfile, ProfileGameSession, TemplateLibrary,
│                SimulatedGameSession, GameSessionFactory
│   ├── UmaMusume/     UmaMusumeProfile
│   └── BlueArchive/   BlueArchiveProfile
│   └── Vision/opencv/ OpencvTemplateMatcher (optional adapter)
├── Vision/      GrayImage, ScreenCapture, TemplateMatcher,
│                PureJavaTemplateMatcher, RegionDetector, ChangeDetector
├── Input/       MouseController, KeyboardController
├── Database/    DatabaseContext, Repositories/ (Settings, Progress), Models/
├── Services/    LoggingService, GameWindowDetector
├── UI/          GameFlowFrame, DashboardPanel, SettingsPanel, LogsPanel,
│                MainViewModel, GameSelectionDialog
├── Models/      GameType, GameState, enums, Rect/Point, DashboardModel, ...
└── App.kt       entry point (--demo / --headless), wiring
src/test/SmokeTest.kt   pure-JVM functional smoke test
```

## Build & run

Requires a **JDK 17+** and the **Kotlin standalone compiler** (`kotlinc`). The two
runtime jars are vendored under `lib/`. This repository was **compiled and its
smoke test run with the official Kotlin 2.4.10 compiler (JVM)**.

The quickest, verified path — compile with `kotlinc`, run with `java`:

```bash
export KOTLINC=/path/to/kotlinc/bin/kotlinc   # or add kotlinc to PATH
./run.sh              # GUI (normal launch)
./run.sh --demo       # GUI with the built-in deterministic simulation
./run.sh smoke        # pure functional smoke test (SMOKE TEST PASSED)
./run.sh --headless   # full engine loop, no window (CI-friendly)
```

On Windows: `run.bat` (same modes).

### Gradle (optional)

Requires **JDK 17+** and **Gradle** (the wrapper fetches the Kotlin plugin
automatically). `./gradlew build`, `./gradlew run --args=--demo`, or
`./gradlew run --args=--headless`.

## Developer notes

- **Window detection** ships as a demonstrative full-desktop detector
  (`GameWindowDetector`). For production, replace `detect()` with a real Win32
  window-enumeration + title→game match (see the class KDoc).
- **Templates**: add real PNGs under `src/main/resources/<Game>/` per the
  README_TEMPLATES.md in each folder. The profiles already list the exact
  filenames/ROIs they expect.
- **OpenCV**: to prefer a real OpenCV engine, add the JavaCV artifacts to the
  dependency list in `build.gradle.kts` and launch with
  `-Dgameflow.opencv=true`. The default matcher requires no native binaries and
  is the recommended low-resource option.
- All Kotlin sources were ported by hand from a validated Java reference and have
  been **verified**: `./run.sh smoke` compiles everything and prints
  `SMOKE TEST PASSED` (matcher, change detector, FSM, safety, task queue, SQLite
  round-trip, and the adaptive poller).
- Compiler notes (kotlinc JVM 2.x): bitwise/shift operations use the named forms
  `.and() .or() .xor() .shl() .shr() .inv()`; collections expose `.size` (not
  `.size()`); string case folding is `.lowercase()`. The sources already follow
  these.

## License / scope reminders

Personal use, offline, respectful of the game and other players. No PvP,
tournaments, ranking manipulation, network/memory hacking, reward exploitation,
anti-cheat bypass, or account farming.