# LastStick

*Think ahead. Swipe wisely. Never take the last stick.* — STRATEGY · LOGIC · AI

An Android game based on **Misère Nim**. Three rows hold 3, 5 and 7 sticks. You and the AI alternate turns (an optional coin flip in Settings, which you trigger by tapping the coin, decides who starts; otherwise you do); on a turn
you remove one or more sticks from **exactly one row**. **Whoever removes the last stick loses.**

Built with Kotlin, Jetpack Compose (Material 3), ViewModel + coroutines, DataStore. No other third-party dependencies.

## Run

Open in Android Studio and run the `app` configuration, or:

```bash
./gradlew :app:installDebug      # build + install on a connected device/emulator
./gradlew :app:testDebugUnitTest # run all unit tests
```

## Architecture (`app/src/main/java/com/example/laststick`)

| Package | Responsibility |
|---|---|
| `game/` | Pure rules: `GameState`, `Move`, `GameRules` (validation, apply, loser detection). No Android types. |
| `ai/` | `MisereNimSolver` (exact solver), `PerfectAi`, `QLearningAgent`, `QTable`, `LearningSnapshot` (text serialisation). |
| `data/` | DataStore-backed `SettingsRepository` / `StatsRepository`, file-based `LearningStore`, `LearningRepository`. |
| `viewmodel/` | `GameViewModel` (turn flow, AI timing, animations phases, stats), `SettingsViewModel`, `StatsViewModel`. |
| `ui/board/` | Pure geometry + swipe selection (`BoardLayout`, `SwipeSelection`, `StickBoard`) – unit tested without Compose. |
| `ui/components`, `ui/screens`, `ui/theme` | Compose UI: menu, game, leaderboard, settings. |

Key decisions:
* `GameState` is the single source of truth; every move is validated in `GameRules`. `StickBoard`/`visuals` are only
  presentation (stick identity + animation) and may lag the game state while sticks animate out.
* `GameViewModel` accepts input only in `Phase.HUMAN_TURN`; the phase flips synchronously on commit, so rapid swipes
  can't double-move. All timing is coroutine `delay` in one job that is cancelled on restart/leave/clear.
* Game flow state survives rotation (ViewModel) and process death (`SavedStateHandle` stores rows, turn and AI mode).

## Swipe selection

The gesture starts on or near a stick; the first stick locks the **row**. Sticks in other rows are **ignored** (the swipe
is not cancelled). Each pointer segment is tested against every stick's bounds (slightly inflated), so fast swipes don't
skip sticks and each stick is selected at most once; sliding back over a selected stick deselects the sticks after it. Release commits the selection as one move; cancel/empty/invalid
selections change nothing. Selected sticks are lifted, outlined and tinted (not colour alone). For TalkBack, the board
exposes custom actions ("Remove N from row R").

## AI

* **Perfect AI (setting on)** – exact Misère Nim: if all piles ≤ 1, the player to move wins iff the pile count is even;
  if exactly one pile is ≥ 2, leave an odd number of 1-piles; otherwise normal-Nim XOR strategy. Unit tests compare it
  with an independent memoised minimax over all 192 reachable positions (and larger boards). From 3-5-7 the first player
  (you) can win with perfect play; the Perfect AI wins whenever you slip.
* **Learning AI (setting off, default)** – tabular Q-learning, written as a two-player (negamax) learner: `Q(s,a)` is the
  outcome *for the player making move `a`*; taking the last stick yields −1, otherwise the target is
  `−γ·max Q(s',·)`. It learns from **both** players' moves (human moves are off-policy samples), uses ε-greedy with
  ε decaying per game down to a floor, persists its table + ε + game count to `files/q_table.txt` (atomic write,
  invalid files are ignored), and can be pre-trained with self-play from Settings. It is **not guaranteed** to become perfect.

## Local statistics

Games played, wins, win rate, streaks and the last 10 results are stored on-device only (the screen says so; there is no
online leaderboard). Abandoned games (left via menu/new game) are not counted.

## Tutorial and languages

* **First-run tutorial:** tooltip-style coach marks (spotlight + bubble with Next/Skip) run once each for the menu, the
  first game (including an animated swipe demo) and Settings. Progress is stored in DataStore; Settings → "Replay tutorial"
  resets it. Targets are registered with `Modifier.coachTarget(...)` (`ui/tutorial/CoachMarks.kt`).
* **Languages:** English and Romanian (`res/values`, `res/values-ro`), selectable in Settings (System default / English /
  Română). The choice applies instantly: `ProvideAppLanguage` re-provides the Compose resource locals with a localised
  context. All user-visible text, including plurals and accessibility labels, lives in string resources. To add a
  language, add `res/values-xx/strings.xml` and an entry in `AppLanguage`.

## Known limitations

* No sound effects (the brief made them optional); haptics can be toggled.
* The tap-and-release of a single stick removes that stick (a 1-stick swipe).
* UI/instrumented tests are not included; selection logic is covered by JVM unit tests and the game was exercised
  manually on an emulator.
