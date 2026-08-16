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

**Capture** — `screenshot` `screenshotElement`

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
3. Copy `gradle/ldlib2-uitest.gradle` into your mod and `apply from:` it — it carries both the
   `-PldTest` wiring and the `verifyUiTest` task, and is inert without `-PldTest`.
4. `gradlew runClient -PldTest=group:<yourmod>`.

## Where the code lives

| Package | Contents |
|---|---|
| `com.lowdragmc.lowdraglib2.uitest` | public API + engine: `UIScenario`, `ScenarioBuilder`, `TestContext`, `ServerContext`, `UITestRunner` |
| `...uitest.input` | `InputDriver`, synthetic/real drivers, `Keys` |
| `...uitest.target` | selector validation, stable element paths, text extraction |
| `...uitest.capture` | framebuffer readback and cropping |
| `...uitest.report` | report model and writer |
| `com.lowdragmc.lowdraglib2.gui.ui.utils` | the seams a run installs itself into: `KeyState`, `CursorState`, `RawInputGate` |
| `com.lowdragmc.lowdraglib2.test.uitest` | LDLib2's own scenarios (dev only) |
