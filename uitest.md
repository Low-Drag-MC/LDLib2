# LDLib2 UI Test Harness

Automated UI tests that run in a **real client**. One command launches the game, creates a world,
opens UIs, clicks and types, waits for server round trips, asserts, screenshots, and exits with a
non-zero code if anything failed.

```
gradlew runClient -PldTest=all
  -> build/ldlib2-uitest/report.json     machine-readable results
  -> build/ldlib2-uitest/report.txt      human summary
  -> build/ldlib2-uitest/screenshots/    full frames + per-element crops
```

Nothing is human-in-the-loop, and nothing is mocked: input goes through the real
`Screen` → `ModularUIWidget` → `UIEventDispatcher` path, and machine UIs open through the real
right-click → menu → S2C packet path.

---

## Status

Not everything here carries the same confidence. Worth knowing before you rely on a step:

| | |
|---|---|
| **Proven by real runs** | opening UIs, selector resolution, transform-correct bounds, hover/click/key/type/focus, the hit-test guard, full-frame and per-element screenshots, the report, failure path, exit codes, Gradle wiring, world bootstrap, scenario isolation, watchdog, background runs (window never focused, physical pointer never moved) |
| **Written, not yet exercised** | `server(...)` and everything server-side (`useBlock`, `waitForSync`, `waitUntilServer`, `setBlock`, `withBlockEntity`, …), `drag`, `scroll`, `doubleClick`, `shiftClick`, `typeInto`, most `ElementQuery` filters, `/ldlib2_autotest` |
| **Not built** | UI-tree JSON snapshots + baseline diff, golden-image diff, JUnit XML |

The middle row is the gap that matters: the code compiles and reads correctly but no scenario has
run it end to end. Closing it means writing a machine-UI scenario and a sync scenario against
`TestBlockEntity` — the worked example below is written against exactly that.

## Writing a scenario

Put it in `src/main/java` (not `src/test` — that source set has no Minecraft on the classpath) and
register it with an annotation. Discovery is an annotation scan across every loaded mod, so a mod
that depends on LDLib2 needs **no changes on the LDLib2 side**.

```java
@LDLRegisterClient(name = "furnace_ui", group = "mymod", registry = UIScenario.REGISTRY,
                   environment = RegistrationEnvironment.DEV_ONLY)
public class FurnaceUiScenario implements UIScenario {

    private static final BlockPos POS = new BlockPos(8, 65, 8);

    @Override
    public void configure(ScenarioOptions o) {
        o.tags("machine").guiScale(3);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.clearArea(POS, 2)
         .setBlock(POS, MyBlocks.FURNACE.get())
         .withBlockEntity(POS, FurnaceBlockEntity.class, be -> be.setFuel(100))
         .awaitClientBlockEntity(POS)
         .useBlock(POS)                                    // real right-click on the server
         .awaitScreen(ModularUIContainerScreen.class)
         .awaitModularUI()
         .click("#btn_start")
         .waitForSync("burn time reaches the client",
                 sc  -> sc.blockEntity(POS, FurnaceBlockEntity.class).getBurnTime(),
                 ctx -> ctx.clientBlockEntity(POS, FurnaceBlockEntity.class).getBurnTime())
         .checkTextContains("#status_label", "Burning")
         .screenshot("running")
         .teardown("close", ctx -> ctx.requirePlayer().closeContainer());
    }
}
```

`RegistrationEnvironment.DEV_ONLY` keeps scenarios out of production builds.

## Running

| Command | Effect |
|---|---|
| `gradlew runClient -PldTest=all` | every registered scenario |
| `-PldTest=furnace_ui` | one by name |
| `-PldTest=a,b,c` | several |
| `-PldTest=group:mymod` | by `@LDLRegisterClient(group=...)` |
| `-PldTest=tag:fast` | by `ScenarioOptions#tags` |
| `-PldTest=regex:.*_ui` | by pattern |
| `-PldTestExclude=slow_one` | subtract from the selection |
| `-PldTestKeepOpen` | leave the game running afterwards |
| `-PldTestWindow=1280x720` | pin the window (default: maximise) |
| `-PldTestGuiScale=3` | run-wide GUI scale (default 2; scenarios can override) |
| `-PldTestInputMode=REAL` | drive the real OS cursor instead of the logical one (**foreground only**) |

`verifyUiTest` is attached to `runClient` with `finalizedBy`, so it runs even if the client crashes,
and **a missing report is itself a failure**.

### Background runs

A run does not need the window focused and never touches your mouse, so you can start one and carry
on working on the same machine.

Input goes straight to `Screen#mouseClicked` and friends, so it never depended on the window manager.
What did was the *pointer*: `ModularUI` recomputes the hovered element every frame from the `mouseX`
/ `mouseY` that `Screen#render` receives, and vanilla derives those from `MouseHandler#xpos()/ypos()`
— the physical cursor. That position now comes from `CursorState`, which a mixin on those two getters
answers with, so hover, tooltips, the carried item and `AbstractContainerScreen`'s hovered slot all
follow the synthetic cursor while the real one stays where you left it. `Minecraft#setScreen(null)`
would normally capture and hide the pointer through `MouseHandler#grabMouse`; that is cancelled for
as long as a virtual cursor is installed. Captures read the main framebuffer, so they are unaffected
by focus or occlusion.

Two things are worth knowing:

- **The window takes focus once, at launch.** It is created by FML's early-loading window, before any
  mod code exists, so nothing here can prevent that. After the run arms itself, nothing raises or
  focuses it again — including the real OS windows the floating-view scenarios open.
- **OS-delivered input is dropped while a run is active.** Vanilla has no focus guard on
  `MouseHandler#onPress` or `KeyboardHandler#keyPress`, so a stray click on the *unfocused* game
  window really does reach the game and lands in the middle of a run. `RawInputGate` closes that for
  the duration; while it is up, the ways to interrupt a run are the window's close button, the
  watchdog, or killing Gradle.

`-PldTestInputMode=REAL` is the one exception and is foreground-only by definition: it warps the real
cursor and waits for the OS to deliver the event, so it focuses the window and moves your mouse.

### Iterating

A cold run spends most of its wall clock on Gradle, mod loading and world creation — none of which
changes between attempts. While working on a scenario:

```
gradlew runClient -PldTest=my_scenario -PldTestKeepOpen
# then, in the running game, as many times as you like:
/ldlib2_autotest run my_scenario
/ldlib2_autotest list
```

## The API

Every builder method returns the builder. The whole vocabulary is convenience over three primitives,
so anything the named steps do not cover is one lambda away:

```java
s.step("anything on the client thread", ctx -> { ... })   // TestContext
 .server("anything on the server thread", sc -> { ... })  // ServerContext, submitted and awaited
 .waitUntil("any condition", ctx -> ...)                  // the single waiting mechanism
```

**World / server** — `setBlock` `fill` `clearArea` `withBlockEntity` `teleportPlayer` `setGameMode`
`giveItem` `setHeldItem` `runCommand` `serverTicks` `awaitClientChunk` `awaitClientBlockEntity`

**Open UI** — `openScreenTest` `openScreen` `openModularUI` `useBlock` `awaitScreen`
`awaitModularUI` `awaitElement` `closeScreen`

**Input** — `hover` `click` `rightClick` `middleClick` `shiftClick` `doubleClick` `clickAt` `scroll`
`drag` `dragTo` `focus` `blur` `key` `keyDown` `keyUp` `type` `typeInto`

**Wait / sync** — `frames` `ticks` `waitMs` `waitUntil` `waitUntilServer` `waitForText`
`waitForTextContains` **`waitForSync`**

**Assert** — `check` `checkServer` `checkEquals` `checkExists` `checkNotExists` `checkCount`
`checkText` `checkTextContains` `checkVisible` `checkHidden` `checkFocused` `checkHovered`
`checkClass` `checkBounds` `checkScreen` `checkValue`

**Capture** — `screenshot` `screenshotElement` `screenshotSurface`

**Structure** — `group` `repeat` `teardown` `teardownServer` `settleMs` `timeoutMs` `log`

### Targeting

Selectors go through LDLib2's own CSS engine: `tag`, `.class`, `#id`, `*`, descendant, `>`,
`:not(...)`, and state pseudo-classes (`:hover` → `.__hovered__`).

Selector **lists (`,`), attribute selectors (`[...]`) and `:nth-child` are rejected with an error**,
because the matcher silently mis-parses them rather than failing. Use `ctx.query(...)` instead:

```java
ctx.query("button").withText("+").excludeInternal().nth(1).one();
```

Every input step hit-tests the target's centre before acting, and fails with
`"resolved X but hit test returned Y"` if the element is occluded, clipped or off screen — so a test
can never silently click nothing and pass.

### A UI in its own OS window

`ctx.el`, `ctx.query` and every input step resolve through the UI behind `Minecraft#screen`. A UI
hosted in a `ModularUIWindow` is behind no screen at all: it takes its input from raw GLFW callbacks
on its own window and is drawn into an off-screen target that never reaches the game's frame. Three
things address that, and each is the only way to do its job:

```java
ctx.in(window.getModularUI(), "button")   // query that window's UI instead of the screen's
   .withText("Save").one();

ctx.input(window)                         // post into the window's own event queue
   .moveTo(element).mouseDown(MOUSE_LEFT);

s.screenshotSurface("label", ctx -> window.surface());   // read back the window's own framebuffer
```

`ctx.input(window)` posts real `OsWindowEvent`s, so the drain, the hit test, the move/resize gesture
check and the dispatch are all the window's own — not a shortcut around them. Coordinates are that
window's GUI space, which is what `ElementBounds` already reports for elements in its UI. Post one
primitive per step, exactly as with the screen driver: the queue is drained once per frame, so
anything that needs a frame to pass in between — a drag — will not start if you batch it.

Two things about it that are load-bearing rather than incidental:

- **One driver per window, kept for the whole scenario.** It remembers where `moveTo` last aimed and
  re-states that position with every button event. A window reads its *own* cached cursor when a
  button arrives, and the platform writes that cache too — so without the re-aim, a physical mouse
  drifting over the window between the aim and the press takes the synthetic click with it.
- **Press and release in the same step for a control that tears down its host** — a window's close
  button, or the debugger's window toggle. LDLib2 controls fire on MOUSE_DOWN, so by the next step
  there is no window left to release into.

## What gets verified

1. **Assertions** — `check(desc, cond)` records expected/actual; a failure marks the step failed and
   the scenario keeps going, so one run surfaces several problems.
2. **Screenshots** — full frames plus per-element crops, taken on the frame *after* the step so they
   never show stale state. A flat-colour image is flagged `SUSPECT` (usually a wrong framebuffer).
   Failures capture automatically.
3. **`report.json`** — schema-versioned, atomically written, with per-step status, timings, target
   bounds, hit-test result, checks, capture paths, and the full environment including the loaded mod
   list.

## Gotchas the harness handles for you

- **Hover is resolved during rendering.** Mouse events on `ModularUIWidget` route to a cached hovered
  element, so a click in the same frame as a cursor move hits the *previous* target. The runner runs
  one step per rendered frame and refreshes the hover before every dispatch.
- **Drags are multi-frame by nature.** A drag only starts when the source sees `MOUSE_LEAVE` while a
  button is held. `drag(...)` expands into eight frame-separated steps and tells you if the drag
  never started.
- **Settle is wall-clock, never frames.** A dev world renders far faster than it ticks, and
  `ModularUI#tick` — which refreshes data-bound labels — runs at 20 Hz. Use `ticks(n)` or a condition
  wait for anything data-driven.
- **The pointer.** `glfwSetCursorPos` moves the pointer of whoever is at the machine, and GLFW ignores
  it outright while the window is unfocused — so a harness built on it can only run in the foreground
  of an idle machine. `CursorState` is the seam that replaces it, the sibling of `KeyState` and there
  for the same reason.
- **Modifier keys.** `glfwGetKey` reads the physical keyboard and nothing in-process can move it, so
  `KeyState` is overridden for the duration of a run. `key(k, MOD_CONTROL)` therefore holds control
  as a real key rather than only setting the event's modifier mask — `UIEvent#isCtrlDown()` reads
  held state, so a mask alone would make ctrl+A behave like a bare A.
- **Stale worlds.** Each run gets a uniquely named world and sweeps `saves/ldtest_*` first — a leaked
  session lock breaks the *next* run, which is much harder to diagnose.

## Adopting it in your own mod

1. Depend on LDLib2 as usual.
2. Write scenarios in `src/main/java` with `@LDLRegisterClient(registry = UIScenario.REGISTRY)`.
3. Copy the `-PldTest` block and the `verifyUiTest` task from LDLib2's `build.gradle`
   (search for `ldlib2.uitest`), or `apply from:` `gradle/ldlib2-uitest.gradle`.
4. `gradlew runClient -PldTest=group:<yourmod>`.

---

# Multi-process tests (dedicated server + N clients)

Everything above runs one client against its integrated server. The multi-process harness tests the
**full wire**: one real dedicated server plus one client per role, connected over localhost, driven
in lockstep. This is how you test what a *second* player sees — `@DescSynced` world sync, per-player
UI sessions, C2S interaction packets, RPC — against the same code paths production runs.

```
gradlew runMpTest -PldMpTest=mp_sync_smoke
  -> build/ldlib2-mptest/report.json      merged, machine-readable
  -> build/ldlib2-mptest/report.txt       human summary
  -> build/ldlib2-mptest/server/          the dedicated server's own report + gradle.log
  -> build/ldlib2-mptest/clientA|B/       each client's report, log and screenshots
```

One command spawns `runMpServer` + `runMpClientA`/`runMpClientB` as child Gradle builds, prepares
`runs/mpServer` (fresh flat world, offline mode, a free port), coordinates everything over a local
TCP hub, merges the three reports, and fails the build via `verifyMpTest` if anything failed —
a missing report included. A dying orchestrator can never leak game processes: every process treats
a dropped hub connection as "shut down now".

## Writing a multi-process scenario

A scenario is an ordered list of **segments**. Each segment is owned by one side; everyone else
waits at a barrier. Inside a `client(..)` block the entire `ScenarioBuilder` DSL above is available
unchanged.

```java
@LDLRegister(name = "my_sync", group = "mymod", registry = MPScenario.REGISTRY,
             environment = RegistrationEnvironment.DEV_ONLY)   // plain @LDLRegister - both dists discover it
public class MySyncScenario implements MPScenario {
    public void configure(MPScenarioOptions o) { o.clients("A", "B"); }

    public void define(MPScenarioBuilder s) {
        s.server("place machine", sc -> { ... })               // dedicated server thread
         .client("A", "open ui and click", b -> b              // ordinary ScenarioBuilder steps
                 .useBlock(POS)                                // real C2S use packet in MP mode
                 .awaitScreen(ModularUIContainerScreen.class)
                 .click("#btn").screenshot("clicked"))
         .serverWaitUntil("server saw it", sc -> ...)          // re-checked every tick
         .server("assert authoritative state", sc -> sc.check(...))
         .client("B", "other client sees it", b -> b
                 .useBlock(POS).waitForText("#label", "43"))
         .sync("value converged on B", "B",                    // cross-process waitForSync
                 sc  -> sc.blockEntity(POS, MyBE.class).getValue(),
                 ctx -> ctx.clientBlockEntity(POS, MyBE.class).getValue())
         .allClients("close", b -> b.closeScreen())            // every client, barrier waits for all
         .teardownServer("clean", sc -> { ... });              // teardown = owned-only, no barriers
    }
}
```

Segment vocabulary: `server` (one task), `serverWaitUntil` (per-tick poll), `client(role, block)`,
`allClients(block)`, `sync`/`syncAll` (client polls until its value equals the server's, fetched as
a probe over the control channel and compared by Gson JSON form — keep values to primitives/strings),
`fetch`/`fetchOn` (below), `teardownServer`/`teardownClient`/`teardownAllClients`.

### Asserting with a tolerance: `fetch`

`sync` compares by JSON equality, which is right for a synced integer and useless for anything
continuous — two processes whose body angles differ by a twentieth of a degree agree about
everything that matters, and no rounding makes a live float converge on the nose. `fetch` copies
the server's number into every client's scratch state once, and leaves the comparison to the client:

```java
.fetch("server.yaw", "the body angle the server holds", Double.class,
        sc -> (double) mover(sc).yaw())
.client("B", "and this client agrees", b -> b.check("within a tick of turning", ctx -> {
    double server = ctx.get("server.yaw");
    double here   = moverOf(ctx, "LDTestA").yaw();
    ctx.log("server " + server + ", here " + here);        // in the report either way
    return Math.abs(Mth.wrapDegrees(server - here)) < 2.0;
}))
```

The reading is taken when the segment runs, so put the `fetch` where you want the sample taken —
what a later step compares against is the server at a known point in the scenario, not at whenever
that step happened to fire. `fetchOn(role, ...)` restricts it to one client. Use `Double.class`
for anything numeric: Gson reads a bare JSON number as a double.

Semantics worth knowing:

- **Soft check failures do not gate other processes** — `check(..)` marks the step failed and the
  run FAIL, exactly like solo. Only a *thrown* step (or a timeout) aborts: the owner broadcasts its
  unfinished segments as ABORTED, every other process's barrier throws, and everyone converges on
  its own teardown, then the next scenario.
- **`server(..)` helpers inside client blocks throw.** A multi-process client has no integrated
  server; world mutation belongs in `s.server(..)` segments, cross-process waits in `s.sync(..)`.
  `useBlock` is the exception — in MP mode it switches to the real client-side interaction path.
- **`ServerContext.player(role)`** resolves a role's player on the dedicated server
  (usernames are `LDTest<role>`); `players()` gives all of them.
- **`define()` runs in every process and must be deterministic** — same segments, same order,
  everywhere. Never branch on live state in `define`; segment bodies are where that happens.
- **A client skips scenarios it has no role in, but its next scenario's clock starts immediately** —
  while the others are still on the earlier scenario, it sits in its first barrier. When scenarios
  with different `clients(..)` sets share a run, size `scenarioTimeoutMs` to cover the scenarios a
  non-participant waits through.

## The dist rule (the one way to break the harness)

`define()` also runs in the **dedicated-server process**, so the scenario class must stay loadable
there. Method *signatures* resolve when a class links; bodies are lazy. In practice:

- Fine: `Consumer<ServerContext>`, `Consumer<ScenarioBuilder>` blocks, inner `ctx -> ...` steps,
  `Predicate<TestContext>`, sync getters returning boxed values, class literals like
  `awaitScreen(Foo.class)` (resolved only when the owning client expands the block).
- **Not fine**: a lambda whose erased signature names a client-only type — e.g.
  `Function<TestContext, Screen>` factories (`openScreen`), or a lambda capturing a client-typed
  local. Use `openScreenTest(name)` or move the code into an `@OnlyIn(Dist.CLIENT)` helper class
  referenced only inside step bodies.

`MPScenarioDistLoadingTest` (a game test, so it runs in every `runGameTestServer`) instantiates and
defines every registered MP scenario on the dedicated dist — a violation fails there with the
scenario's name instead of crashing a live `runMpTest`.

## Running

| Command | Effect |
|---|---|
| `gradlew runMpTest` | every registered MP scenario (`-PldMpTest=all`) |
| `-PldMpTest=<name\|a,b\|group:x\|tag:y\|regex:p>` | the same selection grammar as `-PldTest` |
| `-PldMpClients=A,B` | which client processes to launch (default `A,B`; scenarios needing absent roles are skipped) |

A cold run takes ~1–2 minutes: three child Gradle builds boot in parallel, clients join over
localhost, scenarios run in lockstep, reports merge. Timings inside client blocks are the solo
defaults; barriers use the scenario budget (`MPScenarioOptions#scenarioTimeoutMs`, default 5 min)
because they legitimately span another process's whole segment.

Debugging order when a run fails: `build/ldlib2-mptest/report.txt` (which role, which segment),
then that role's `report.json` and screenshots, then its `gradle.log`. The orchestrator's own
`[mptest]` lines in the build output show the hub's view: who connected, who joined, which
scenario ended how, who exited with what.

## Where the multi-process code lives

| Piece | Location |
|---|---|
| public API (`MPScenario`, `MPScenarioBuilder`, `MPScenarioOptions`, `MPSegment`) | `com.lowdragmc.lowdraglib2.uitest.mp` |
| control protocol + hub client + orchestrator (no Minecraft imports) | `...uitest.mp` (`MPMessages`, `MPHubClient`, `MPTestOrchestrator`) |
| client-side compiler/session glue | `com.lowdragmc.lowdraglib2.uitest.MPClientSession` |
| dedicated-server runner + bootstrap | `...uitest.MPServerRunner`, `...uitest.MPServerBootstrap` |
| Gradle wiring (`runMpTest`, `verifyMpTest`, the three child runs) | `gradle/ldlib2-mptest.gradle` |
| LDLib2's own MP scenarios | `com.lowdragmc.lowdraglib2.test.uitest.mp` |

Adopting it downstream mirrors the solo harness: write `MPScenario`s in `src/main/java`, copy
`gradle/ldlib2-mptest.gradle` next to your `runs { }` block, and run
`gradlew runMpTest -PldMpTest=group:<yourmod>`.

## Where the code lives

| Package | Contents |
|---|---|
| `com.lowdragmc.lowdraglib2.uitest` | public API + engine: `UIScenario`, `ScenarioBuilder`, `TestContext`, `ServerContext`, `UITestRunner` |
| `...uitest.input` | `InputDriver`, synthetic/real drivers, `Keys`, `WindowInput` (a UI in its own OS window) |
| `...uitest.target` | selector validation, stable element paths, text extraction |
| `...uitest.capture` | framebuffer readback and cropping |
| `...uitest.report` | report model and writer |
| `com.lowdragmc.lowdraglib2.gui.ui.utils` | the seams a run installs itself into: `KeyState`, `CursorState`, `RawInputGate` |
| `com.lowdragmc.lowdraglib2.test.uitest` | LDLib2's own scenarios (dev only) |
