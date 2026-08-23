package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.client.window.OsWindowHints;
import com.lowdragmc.lowdraglib2.gui.ui.utils.CursorOverlay;
import com.lowdragmc.lowdraglib2.uitest.capture.CaptureRequest;
import com.lowdragmc.lowdraglib2.uitest.capture.FrameCapture;
import com.lowdragmc.lowdraglib2.uitest.input.InputDriver;
import com.lowdragmc.lowdraglib2.uitest.mp.MPRunConfig;
import com.lowdragmc.lowdraglib2.uitest.report.ReportWriter;
import com.lowdragmc.lowdraglib2.uitest.report.RunReport;
import com.lowdragmc.lowdraglib2.uitest.target.ElementPath;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The engine. Drives every scenario from a post-render frame hook, one step per frame.
 *
 * <p>The frame hook is {@code RenderFrameEvent.Post} rather than a screen render event, because a
 * screen event stops firing the moment no screen is open — which is exactly what happens right after
 * a world finishes loading, so the harness would stall there forever and only resume if a human
 * pressed escape.
 */
@OnlyIn(Dist.CLIENT)
public final class UITestRunner {

    private enum Phase {
        WAIT_GAME_READY,
        PIN_OPTIONS,
        CREATE_WORLD,
        AWAIT_WORLD,
        PIN_WORLD,
        /** Multi-process only: join the dedicated server instead of creating a world. */
        MP_CONNECT,
        MP_AWAIT_JOIN,
        MP_AWAIT_BEGIN,
        NEXT_SCENARIO,
        RUN_SCENARIO,
        FINISH,
        /** Report written, shutdown handed off. The frame loop must keep turning but do nothing. */
        EXITING
    }

    private static UITestRunner active;

    private final RunConfig config;
    private final RunReport report = new RunReport();
    private final Deque<ScenarioEntry> queue = new ArrayDeque<>();
    private final InputDriver inputDriver;
    private final String runId;
    private final String worldName;
    /** Present when this client is one process of a multi-process run. */
    @Nullable
    private final MPClientSession mpSession;

    private Phase phase = Phase.WAIT_GAME_READY;
    private long settleUntilNanos;
    private boolean inStep;
    private boolean awaitingHubConfigLogged;
    @Nullable
    private ScenarioRun currentRun;
    @Nullable
    private TestContext currentContext;
    @Nullable
    private RunReport.StepReport currentStepReport;
    private long stepStartedNanos;
    /**
     * A queue, not a slot: a step is free to ask for several captures, and each needs its own frame
     * so the image matches the state at the time it was requested. A single slot would silently drop
     * all but the last, which is the kind of loss you only notice when the screenshot you needed is
     * missing from a failure report.
     */
    private final Deque<CaptureRequest> pendingCaptures = new ArrayDeque<>();
    private long lastHeartbeatNanos = System.nanoTime();
    private long runStartedNanos;
    /** Read by the watchdog thread, hence volatile. */
    private volatile boolean shuttingDown;

    private record ScenarioEntry(String name, String group, UIScenario scenario, ScenarioOptions options,
                                 Class<?> scenarioClass) {
    }

    private UITestRunner(RunConfig config, String runId, @Nullable MPClientSession mpSession) {
        this.config = config;
        this.runId = runId;
        this.worldName = WorldBootstrap.worldNameFor(runId);
        this.inputDriver = InputDriver.of(config.inputMode());
        this.mpSession = mpSession;
    }

    @Nullable
    public static UITestRunner active() {
        return active;
    }

    public static boolean isRunning() {
        return active != null && active.phase != Phase.FINISH && active.phase != Phase.EXITING;
    }

    /**
     * Starts a run if the launch asked for one. Called once, lazily, from the first rendered frame —
     * not from a mod-loading event, because registries and the window are not ready that early.
     */
    static void bootstrapIfRequested() {
        var mpConfig = MPRunConfig.fromSystemProperties();
        if (mpConfig != null && !mpConfig.isServer()) {
            var session = new MPClientSession(mpConfig);
            session.connectAsync();
            active = new UITestRunner(RunConfig.forMultiProcess(mpConfig.outDir()),
                    "mp_" + mpConfig.role(), session);
            active.begin();
            return;
        }
        var config = RunConfig.fromSystemProperties();
        if (config == null) return;
        // A pid-qualified id keeps two concurrent runs from sharing a world directory or an output dir.
        var runId = ProcessHandle.current().pid() + "_" + Integer.toHexString(System.identityHashCode(config));
        active = new UITestRunner(config, runId, null);
        active.begin();
    }

    /**
     * Starts a run inside an already-running game, reusing the loaded world and leaving it loaded.
     *
     * <p>Backs {@code /ldlib2_autotest run <name>}. A cold run spends most of its wall clock on Gradle,
     * mod loading and world creation, none of which changes between attempts — so while iterating on
     * a scenario, launch once with {@code -PldTestKeepOpen} and re-run from here.
     *
     * @return an error message, or {@code null} if the run started
     */
    @Nullable
    public static String runInteractive(String selection) {
        if (isRunning()) {
            return "A UI test run is already in progress";
        }
        var runner = new UITestRunner(RunConfig.interactive(selection), "interactive", null);
        runner.begin();
        runner.collectEnvironment(Minecraft.getInstance());
        runner.collectScenarios();
        if (runner.queue.isEmpty()) {
            return "Selection '" + selection + "' matched no scenarios";
        }
        // An interactive run reuses whatever world is loaded rather than creating one, so say so up
        // front instead of letting the scenario fail somewhere in the middle on a null player.
        if (Minecraft.getInstance().level == null
                && runner.queue.stream().anyMatch(entry -> entry.options().requiresWorld())) {
            return "Selection '" + selection + "' needs a loaded world; join one first";
        }
        runner.inputDriver.install();
        runner.installBackgroundMode();
        runner.phase = Phase.NEXT_SCENARIO;
        active = runner;
        return null;
    }

    /** Registered scenario names, for command completion and {@code /ldlib2_autotest list}. */
    public static List<String> registeredScenarioNames() {
        var registry = LDLib2Registries.UI_SCENARIOS;
        if (registry == null) return List.of();
        var names = new ArrayList<String>();
        for (var holder : registry) {
            names.add(holder.annotation().name());
        }
        names.sort(String::compareTo);
        return names;
    }

    private void begin() {
        runStartedNanos = System.nanoTime();
        report.runId = runId;
        report.startedAt = System.currentTimeMillis();
        report.selection = config.selection();
        LDLib2.LOGGER.info("[uitest] run {} armed with selection '{}', output {}",
                runId, config.selection(), config.outDir());
    }

    // region frame loop

    /**
     * Called once per rendered frame.
     *
     * <p>The re-entrancy guard covers the whole body, not just step execution: several of the things
     * a phase does — world creation above all — pump the render loop internally, which fires this
     * event again from inside the call. Without the guard, world creation restarts on every nested
     * frame and the run never progresses past it.
     */
    public void onFrame() {
        if (inStep) return;
        inStep = true;
        try {
            onFrameInternal();
        } finally {
            inStep = false;
        }
    }

    private void onFrameInternal() {
        lastHeartbeatNanos = System.nanoTime();
        var minecraft = Minecraft.getInstance();

        // A resource reload replaces the screen and blocks input; nothing we do would stick.
        if (minecraft.getOverlay() != null) return;

        // A dead orchestrator must never leave game processes running. Going through fatal/FINISH
        // still writes whatever report exists, which is more than an orphaned process would leave.
        if (mpSession != null && mpSession.hub().isLost()
                && phase != Phase.FINISH && phase != Phase.EXITING) {
            fatal("the control hub connection was lost");
            return;
        }

        if (!pendingCaptures.isEmpty()) {
            servicePendingCapture(pendingCaptures.poll());
            return;
        }

        if (System.nanoTime() < settleUntilNanos) return;

        switch (phase) {
            case WAIT_GAME_READY -> {
                if (minecraft.screen instanceof TitleScreen) {
                    phase = Phase.PIN_OPTIONS;
                } else if (minecraft.screen instanceof AccessibilityOnboardingScreen) {
                    // A fresh game directory (an mp client's first launch, a clean checkout) boots
                    // into accessibility onboarding, not the title. Dismiss it the way its button
                    // would, or the run sits here forever without a single log line.
                    LDLib2.LOGGER.info("[uitest] dismissing first-launch accessibility onboarding");
                    minecraft.options.onboardAccessibility = false;
                    minecraft.options.save();
                    minecraft.setScreen(new TitleScreen());
                } else if (elapsedMsSince(runStartedNanos) > 180_000) {
                    fatal("never reached the title screen (stuck on "
                            + (minecraft.screen == null ? "no screen" : minecraft.screen.getClass().getName()) + ")");
                }
            }
            case PIN_OPTIONS -> {
                // Multi-process: the selection and game port arrive in the hub's config message, so
                // scenario collection cannot happen until it does.
                if (mpSession != null && mpSession.hub().config() == null) {
                    if (!awaitingHubConfigLogged) {
                        awaitingHubConfigLogged = true;
                        LDLib2.LOGGER.info("[mptest] waiting for the hub's run configuration");
                    }
                    if (elapsedMsSince(runStartedNanos) > 120_000) {
                        fatal("the hub never sent its configuration");
                    }
                    return;
                }
                pinOptions(minecraft);
                collectEnvironment(minecraft);
                collectScenarios();
                // Here rather than after world creation: a run whose scenarios all opt out of a world
                // skips that phase entirely, and would otherwise never get the key-state override.
                inputDriver.install();
                installBackgroundMode();
                if (mpSession != null) {
                    // Join even with an empty queue: the hub only releases `begin` once every
                    // launched client is in the world, so a non-participating client that bailed
                    // here would deadlock the ones with work to do.
                    if (queue.isEmpty()) {
                        LDLib2.LOGGER.warn("[uitest] no MP scenarios for role '{}' - joining anyway",
                                mpSession.role());
                    }
                    phase = Phase.MP_CONNECT;
                } else if (queue.isEmpty()) {
                    LDLib2.LOGGER.error("[uitest] selection '{}' matched no scenarios", config.selection());
                    phase = Phase.FINISH;
                } else {
                    phase = queue.stream().anyMatch(entry -> entry.options().requiresWorld())
                            ? Phase.CREATE_WORLD
                            : Phase.NEXT_SCENARIO;
                }
                // One frame for the resize to take effect before anything measures the viewport.
                settle(50);
            }
            case CREATE_WORLD -> {
                WorldBootstrap.deleteStaleWorlds(minecraft);
                LDLib2.LOGGER.info("[uitest] creating world '{}'", worldName);
                // Advance before the call, not after: world creation runs its own render loop.
                phase = Phase.AWAIT_WORLD;
                WorldBootstrap.createFreshLevel(minecraft, worldName);
            }
            case AWAIT_WORLD -> {
                if (minecraft.player != null && minecraft.level != null
                        && minecraft.getSingleplayerServer() != null) {
                    phase = Phase.PIN_WORLD;
                } else if (elapsedMsSince(runStartedNanos) > 180_000) {
                    fatal("world never finished loading");
                }
            }
            case PIN_WORLD -> {
                var server = minecraft.getSingleplayerServer();
                if (server != null) {
                    server.execute(() -> WorldBootstrap.pinWorldRules(server));
                }
                // Close the "downloading terrain" screen so the first scenario starts from nothing open.
                minecraft.setScreen(null);
                phase = Phase.NEXT_SCENARIO;
                // The gamerule commands were queued onto the server thread; give them a tick to land
                // so the first scenario does not race "kill @e" spawning particles into a capture.
                settle(100);
            }
            case MP_CONNECT -> {
                assert mpSession != null;
                var gamePort = mpSession.gamePort();
                LDLib2.LOGGER.info("[mptest] '{}' connecting to 127.0.0.1:{} (attempt {})",
                        mpSession.role(), gamePort, mpSession.connectAttempts() + 1);
                mpSession.beginConnectAttempt();
                // Advance before the call: startConnecting swaps screens and pumps events.
                phase = Phase.MP_AWAIT_JOIN;
                ConnectScreen.startConnecting(new TitleScreen(), minecraft,
                        new ServerAddress("127.0.0.1", gamePort),
                        new ServerData("LDLib2 MP Test", "127.0.0.1:" + gamePort, ServerData.Type.OTHER),
                        false, null);
            }
            case MP_AWAIT_JOIN -> {
                assert mpSession != null;
                if (minecraft.player != null && minecraft.level != null) {
                    LDLib2.LOGGER.info("[mptest] '{}' joined the dedicated server", mpSession.role());
                    minecraft.setScreen(null);
                    mpSession.sendJoined();
                    phase = Phase.MP_AWAIT_BEGIN;
                    settle(100);
                } else if (minecraft.screen instanceof DisconnectedScreen) {
                    if (mpSession.connectAttempts() >= 10) {
                        fatal("could not join the dedicated server after "
                                + mpSession.connectAttempts() + " attempts");
                    } else {
                        LDLib2.LOGGER.warn("[mptest] join attempt failed; retrying");
                        phase = Phase.MP_CONNECT;
                        settle(2_000);
                    }
                } else if (elapsedMsSince(runStartedNanos) > 300_000) {
                    fatal("never joined the dedicated server");
                }
            }
            case MP_AWAIT_BEGIN -> {
                assert mpSession != null;
                if (mpSession.hub().isBegun()) {
                    LDLib2.LOGGER.info("[mptest] '{}' starting scenarios", mpSession.role());
                    phase = Phase.NEXT_SCENARIO;
                } else if (elapsedMsSince(runStartedNanos) > 420_000) {
                    fatal("the hub never sent 'begin' - did another client fail to join?");
                }
            }
            case NEXT_SCENARIO -> startNextScenario(minecraft);
            case RUN_SCENARIO -> tickScenario();
            case FINISH -> finish(minecraft);
            case EXITING -> {
                // Nothing to do, but the frame loop must keep turning: Minecraft's shutdown wants a
                // live render thread, and keepOpen leaves the game usable.
            }
        }
    }

    private void startNextScenario(Minecraft minecraft) {
        // Whatever the previous scenario left open must not leak into the next one.
        if (minecraft.player != null) minecraft.player.closeContainer();
        minecraft.setScreen(null);

        var entry = queue.poll();
        if (entry == null) {
            phase = Phase.FINISH;
            return;
        }

        // Applied per scenario: the right GUI scale depends on the UI being tested, and a run may
        // cover both a small panel and a full-screen editor.
        int scale = entry.options().guiScaleOverride() >= 0
                ? entry.options().guiScaleOverride() : config.guiScale();
        if (minecraft.options.guiScale().get() != scale) {
            minecraft.options.guiScale().set(scale);
            minecraft.resizeDisplay();
            // Let the relayout land before the first step measures anything.
            settle(50);
        }

        var scenarioReport = new RunReport.ScenarioReport();
        scenarioReport.name = entry.name();
        scenarioReport.group = entry.group();
        scenarioReport.className = entry.scenarioClass().getName();
        scenarioReport.tags.addAll(entry.options().tags());
        report.scenarios.add(scenarioReport);

        var builder = new ScenarioBuilder();
        try {
            entry.scenario().define(builder);
        } catch (Throwable t) {
            scenarioReport.status = RunReport.Status.ERROR;
            scenarioReport.error = RunReport.ErrorInfo.of(t);
            LDLib2.LOGGER.error("[uitest] scenario '{}' failed while being defined", entry.name(), t);
            if (mpSession != null) {
                // Skipping a scenario without a word would leave the other processes waiting out
                // their barrier budgets on segments this client will never run.
                mpSession.onScenarioFinished(entry.name(), scenarioReport.status);
            }
            return;
        }

        currentRun = new ScenarioRun(entry.name(), entry.options(), builder.steps(),
                builder.teardownSteps(), scenarioReport);
        currentRun.begin(System.nanoTime());
        currentContext = new TestContext(currentRun, this);
        phase = Phase.RUN_SCENARIO;
        LDLib2.LOGGER.info("[uitest] --- scenario '{}' ({} steps)", entry.name(), builder.steps().size());
    }

    /**
     * One step per frame. Everything about this method's shape exists to guarantee that: a step
     * always observes a UI that has been rendered at least once since the previous step, which is
     * what makes hover, layout and the pose caches valid when it reads them.
     */
    private void tickScenario() {
        var run = currentRun;
        var ctx = currentContext;
        if (run == null || ctx == null) {
            phase = Phase.NEXT_SCENARIO;
            return;
        }

        long now = System.nanoTime();
        if (run.elapsedNanos(now) > run.options.scenarioTimeoutMs() * 1_000_000L && !run.isInTeardown()) {
            LDLib2.LOGGER.error("[uitest] scenario '{}' exceeded its {} ms budget", run.name,
                    run.options.scenarioTimeoutMs());
            run.report.status = RunReport.Status.worst(run.report.status, RunReport.Status.ERROR);
            if (run.report.error == null) {
                run.report.error = RunReport.ErrorInfo.of(
                        new IllegalStateException("scenario timed out after " + run.options.scenarioTimeoutMs() + " ms"));
            }
            run.abortToTeardown();
        }

        var step = run.peek();
        if (step == null) {
            finishScenario(run);
            return;
        }

        if (run.current() != step) {
            currentStepReport = new RunReport.StepReport();
            currentStepReport.index = run.stepIndex();
            currentStepReport.name = step.name;
            currentStepReport.kind = step.kind.name();
            currentStepReport.group = step.group;
            currentStepReport.settleMs = run.effectiveSettleMs(step);
            run.report.steps.add(currentStepReport);
            stepStartedNanos = now;
        }
        run.startAttempt(step, now);
        ctx.bindStep(currentStepReport);

        inStep = true;
        Throwable failure = null;
        try {
            step.body.run(ctx);
        } catch (Throwable t) {
            failure = t;
        } finally {
            inStep = false;
        }

        if (failure == null && run.repeatRequested) {
            if (run.waitedNanos(now) > run.effectiveTimeoutMs(step) * 1_000_000L) {
                failure = new IllegalStateException("timed out after " + run.effectiveTimeoutMs(step)
                        + " ms waiting for: " + run.waitingFor);
                currentStepReport.waitingFor = run.waitingFor;
            } else {
                // Same step again next frame. No settle: a wait should poll as fast as frames allow.
                return;
            }
        }

        completeStep(run, step, failure);
    }

    private void completeStep(ScenarioRun run, Step step, @Nullable Throwable failure) {
        var stepReport = currentStepReport;
        stepReport.attempts = run.attempt();
        stepReport.durationMs = (System.nanoTime() - stepStartedNanos) / 1_000_000L;

        boolean checksFailed = stepReport.checks.stream().anyMatch(check -> !check.passed);
        if (failure != null) {
            stepReport.status = RunReport.Status.ERROR;
            stepReport.error = RunReport.ErrorInfo.of(failure);
            LDLib2.LOGGER.error("[uitest] {} / step {} '{}' errored: {}", run.name, stepReport.index,
                    step.name, failure.toString());
        } else if (checksFailed) {
            stepReport.status = RunReport.Status.FAIL;
            stepReport.checks.stream().filter(check -> !check.passed).forEach(check ->
                    LDLib2.LOGGER.error("[uitest] {} / CHECK FAILED: {}{}", run.name, check.desc,
                            check.expected == null ? ""
                                    : " (expected " + check.expected + ", actual " + check.actual + ")"));
        } else {
            stepReport.status = RunReport.Status.PASS;
        }

        run.report.status = RunReport.Status.worst(run.report.status, stepReport.status);

        boolean bad = failure != null || checksFailed;
        if (bad && run.options.captureOnFailure()) {
            pendingCaptures.add(new CaptureRequest(CaptureRequest.Kind.ERROR, run.name,
                    "error", stepReport.index, stepReport, null));
        } else if (run.options.captureEveryStep()) {
            pendingCaptures.add(new CaptureRequest(CaptureRequest.Kind.FULL, run.name,
                    "step", stepReport.index, stepReport, null));
        }

        run.advance();

        if (failure != null && run.options.errorPolicy() == ErrorPolicy.ABORT_SCENARIO && !run.isInTeardown()) {
            LDLib2.LOGGER.error("[uitest] aborting scenario '{}'; running teardown", run.name);
            if (run.report.error == null) run.report.error = stepReport.error;
            run.abortToTeardown();
        }

        settle(run.effectiveSettleMs(step));
    }

    private void finishScenario(ScenarioRun run) {
        run.report.durationMs = run.elapsedNanos(System.nanoTime()) / 1_000_000L;
        LDLib2.LOGGER.info("[uitest] --- scenario '{}' {} in {} ms", run.name, run.report.status,
                run.report.durationMs);
        if (mpSession != null) {
            // Broadcast anything this client never announced (abort, timeout) so the other
            // processes' barriers fail fast instead of waiting out their own timeouts.
            mpSession.onScenarioFinished(run.name, run.report.status);
        }
        currentRun = null;
        currentContext = null;
        currentStepReport = null;
        phase = Phase.NEXT_SCENARIO;
    }

    // endregion

    // region captures

    /** Queues a full-frame capture. Serviced next frame, so it shows the state the step produced. */
    public void requestFullCapture(ScenarioRun run, RunReport.StepReport stepReport, String label) {
        enqueueCapture(new CaptureRequest(CaptureRequest.Kind.FULL, run.name, label,
                stepReport.index, stepReport, null));
    }

    public void requestElementCapture(ScenarioRun run, RunReport.StepReport stepReport, String label,
                                      ElementRef element) {
        enqueueCapture(new CaptureRequest(CaptureRequest.Kind.ELEMENT, run.name, label,
                stepReport.index, stepReport, element));
    }

    private void enqueueCapture(CaptureRequest request) {
        pendingCaptures.add(request);
        // The frame about to be rendered is the one this reads back, so the stand-in pointer has to go
        // now: a real cursor is composited by the window manager and never lands in a framebuffer.
        CursorOverlay.setHidden(true);
    }

    private void servicePendingCapture(@Nullable CaptureRequest request) {
        if (request == null) return;

        NativeImage frame = null;
        NativeImage cropped = null;
        try {
            frame = FrameCapture.grab();
            var fileName = "%02d_%s".formatted(request.stepIndex, sanitize(request.label));
            var directory = config.outDir().resolve("screenshots").resolve(sanitize(request.scenarioName));

            var reference = new RunReport.CaptureRef();
            reference.kind = request.kind.name();

            if (request.kind == CaptureRequest.Kind.ELEMENT && request.element != null) {
                cropped = FrameCapture.crop(frame, request.element.bounds(), 2f);
                if (cropped == null) {
                    request.stepReport.log.add("element capture '" + request.label
                            + "' skipped: the element is entirely off screen");
                    return;
                }
                fileName += "__" + ElementPath.slug(request.element.path());
                reference.elementPath = request.element.path();
                reference.suspect = FrameCapture.write(cropped, directory.resolve(fileName + ".png"));
            } else {
                reference.suspect = FrameCapture.write(frame, directory.resolve(fileName + ".png"));
            }

            reference.path = config.outDir().relativize(directory.resolve(fileName + ".png"))
                    .toString().replace('\\', '/');
            request.stepReport.captures.add(reference);
            report.totals.captures++;
            if (reference.suspect) {
                LDLib2.LOGGER.warn("[uitest] capture '{}' is a single flat colour - wrong framebuffer?",
                        reference.path);
            }
        } catch (Throwable t) {
            LDLib2.LOGGER.error("[uitest] capture '{}' failed", request.label, t);
            request.stepReport.log.add("capture '" + request.label + "' failed: " + t);
        } finally {
            FrameCapture.closeQuietly(cropped);
            FrameCapture.closeQuietly(frame);
            // Queued captures are serviced one per frame, so the pointer stays away until the last one
            // has been read back.
            if (pendingCaptures.isEmpty()) {
                CursorOverlay.setHidden(false);
            }
        }
    }

    // endregion

    private void finish(Minecraft minecraft) {
        shuttingDown = true;
        inputDriver.uninstall();
        uninstallBackgroundMode();
        report.finishedAt = System.currentTimeMillis();
        report.durationMs = elapsedMsSince(runStartedNanos);
        ReportWriter.finalise(report);
        ReportWriter.write(report, config.outDir());
        ReportWriter.logSummary(report);

        if (mpSession != null) {
            // After the report hit disk: the orchestrator merges from files once processes exit.
            mpSession.onRunDone(report.status);
        }

        if (config.keepOpen()) {
            LDLib2.LOGGER.info("[uitest] keepOpen is set - leaving the game running");
            phase = Phase.EXITING;
            return;
        }

        boolean failed = !RunReport.Status.PASS.equals(report.status);
        LDLib2.LOGGER.info("[uitest] shutting down with exit code {}", failed ? 1 : 0);
        phase = Phase.EXITING;
        armExitCode(failed ? 1 : 0);
        minecraft.setScreen(null);

        // Minecraft#stop only flips the main loop's running flag. Everything else - unloading the
        // level, halting the integrated server, releasing the session lock - happens on the way out
        // of the loop, on the render thread, where it belongs.
        //
        // Do NOT disconnect() or System.exit() from here. Both are called from inside a frame event,
        // which is inside runTick(); disconnect() runs its own nested render loop and System.exit()
        // parks the render thread inside a shutdown that is waiting on that same thread. Either one
        // hangs the process at 100% of the way through a passing run.
        minecraft.stop();
    }

    /**
     * Makes a failing run exit non-zero.
     *
     * <p>The clean shutdown path always exits 0, so the code is forced from a shutdown hook. This is
     * a secondary signal only — the Gradle {@code verifyUiTest} task reads {@code report.json} and is
     * authoritative, precisely because it also catches the case where the client dies before writing
     * a report at all.
     */
    private void armExitCode(int exitCode) {
        if (exitCode == 0) return;
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> Runtime.getRuntime().halt(exitCode), "ldlib2-uitest-exit-code"));
    }

    private void fatal(String reason) {
        LDLib2.LOGGER.error("[uitest] fatal: {}", reason);
        report.status = RunReport.Status.ERROR;
        var scenarioReport = new RunReport.ScenarioReport();
        scenarioReport.name = "<bootstrap>";
        scenarioReport.status = RunReport.Status.ERROR;
        scenarioReport.error = RunReport.ErrorInfo.of(new IllegalStateException(reason));
        report.scenarios.add(scenarioReport);
        phase = Phase.FINISH;
    }

    /** Whether the watchdog should stand down — shutdown legitimately stops producing frames. */
    boolean isShuttingDown() {
        return shuttingDown;
    }

    /** Called by the watchdog when no frame has arrived for too long. */
    void reportHang(String threadDump) {
        report.status = RunReport.Status.HUNG;
        var scenarioReport = new RunReport.ScenarioReport();
        scenarioReport.name = currentRun == null ? "<no scenario>" : currentRun.name;
        scenarioReport.status = RunReport.Status.HUNG;
        scenarioReport.error = new RunReport.ErrorInfo();
        scenarioReport.error.type = "Hang";
        scenarioReport.error.message = "no rendered frame for " + config.watchdogSeconds() + "s";
        scenarioReport.error.stackTrace = threadDump;
        report.scenarios.add(scenarioReport);
        report.finishedAt = System.currentTimeMillis();
        ReportWriter.finalise(report);
        ReportWriter.write(report, config.outDir());
    }

    long lastHeartbeatNanos() {
        return lastHeartbeatNanos;
    }

    RunConfig config() {
        return config;
    }

    public InputDriver input() {
        return inputDriver;
    }

    /** Whether this run is one client of a multi-process test. */
    public boolean isMultiProcess() {
        return mpSession != null;
    }

    /** This client's role in a multi-process run, or {@code null} in solo/interactive runs. */
    @Nullable
    public String mpRole() {
        return mpSession == null ? null : mpSession.role();
    }

    private void settle(long millis) {
        settleUntilNanos = System.nanoTime() + Math.max(0, millis) * 1_000_000L;
    }

    private static long elapsedMsSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    /**
     * A default-sized window makes a multi-pane UI unreadable in a screenshot, which is the main
     * thing this harness produces — so maximise unless an explicit size was requested. Pinning a
     * size is the right call once captures are compared between machines, since the maximised size
     * depends on the display.
     */
    private void pinOptions(Minecraft minecraft) {
        var window = minecraft.getWindow();
        var mayTakeFocus = mayTakeFocus();
        if (!config.maximizeWindow()) {
            GLFW.glfwSetWindowSize(window.getWindow(), config.windowWidth(), config.windowHeight());
        } else if (mayTakeFocus) {
            GLFW.glfwMaximizeWindow(window.getWindow());
        } else {
            // glfwMaximizeWindow is ShowWindow(SW_MAXIMIZE) on Win32, which *activates* the window -
            // as much a focus theft as glfwFocusWindow, and one that fires on the default path.
            // Filling the monitor's work area gives the same big, readable frame; glfwSetWindowPos and
            // glfwSetWindowSize both pass SWP_NOACTIVATE.
            fillWorkArea(window.getWindow());
        }
        if (mayTakeFocus) {
            GLFW.glfwFocusWindow(window.getWindow());
        }

        var options = minecraft.options;
        options.guiScale().set(config.guiScale());
        // A run launched from a terminal does not hold focus; if the game pauses the harness stalls
        // forever waiting for a frame that never comes.
        options.pauseOnLostFocus = false;
        options.renderDistance().set(4);
        options.entityShadows().set(false);
        // Hides the hotbar, crosshair and chat backlog. Screens still render, so this only removes
        // things that would sit in frame behind whatever the scenario is actually looking at.
        options.hideGui = true;
        minecraft.resizeDisplay();
        LDLib2.LOGGER.info("[uitest] window {}x{}, guiScale {}, focus {}",
                window.getScreenWidth(), window.getScreenHeight(), window.getGuiScale(), mayTakeFocus);
    }

    /**
     * Whether this run may take the operating system's focus and raise its window.
     *
     * <p>Real input is the one mode that has to own the window: GLFW delivers cursor callbacks to
     * nothing else and ignores {@code glfwSetCursorPos} on an unfocused one. Every other mode stays
     * out of the way of whoever is using the machine.
     */
    private boolean mayTakeFocus() {
        return config.inputMode() == InputMode.REAL;
    }

    /**
     * The part of staying out of the user's way that is not the input driver's to own. The driver
     * installs the virtual cursor and the raw-input gate; this is only the window system.
     */
    private void installBackgroundMode() {
        OsWindowHints.setFocusOnShow(mayTakeFocus());
    }

    private void uninstallBackgroundMode() {
        OsWindowHints.setFocusOnShow(true);
    }

    /**
     * Sizes the window to the monitor's work area, as maximising would, but without activating it.
     * Leaves the window alone if the platform cannot say how big that area is.
     */
    private static void fillWorkArea(long handle) {
        var monitor = GLFW.glfwGetPrimaryMonitor();
        if (monitor == 0L) return;
        int[] areaX = new int[1], areaY = new int[1], areaWidth = new int[1], areaHeight = new int[1];
        GLFW.glfwGetMonitorWorkarea(monitor, areaX, areaY, areaWidth, areaHeight);
        // The frame - title bar and borders - sits outside the content area the size below applies to.
        int[] left = new int[1], top = new int[1], right = new int[1], bottom = new int[1];
        GLFW.glfwGetWindowFrameSize(handle, left, top, right, bottom);
        var width = areaWidth[0] - left[0] - right[0];
        var height = areaHeight[0] - top[0] - bottom[0];
        if (width <= 0 || height <= 0) return;
        GLFW.glfwSetWindowPos(handle, areaX[0] + left[0], areaY[0] + top[0]);
        GLFW.glfwSetWindowSize(handle, width, height);
    }

    private void collectEnvironment(Minecraft minecraft) {
        var window = minecraft.getWindow();
        var environment = report.environment;
        environment.minecraft = FMLLoader.versionInfo().mcVersion();
        environment.neoforge = FMLLoader.versionInfo().neoForgeVersion();
        environment.java = System.getProperty("java.version", "");
        environment.os = System.getProperty("os.name", "") + " " + System.getProperty("os.version", "");
        environment.guiScale = (int) window.getGuiScale();
        environment.windowWidth = window.getGuiScaledWidth();
        environment.windowHeight = window.getGuiScaledHeight();
        environment.framebufferWidth = window.getScreenWidth();
        environment.framebufferHeight = window.getScreenHeight();
        environment.inputMode = config.inputMode().name();
        // Worth recording: a dev runtime loads the whole localImplementation set, and any of those
        // can change layout or the render pipeline under a capture.
        ModList.get().forEachModContainer((id, container) ->
                environment.mods.add(id + "@" + container.getModInfo().getVersion()));
        environment.mods.sort(String::compareTo);
    }

    private void collectScenarios() {
        if (mpSession != null) {
            for (var entry : mpSession.compileSelection()) {
                queue.add(new ScenarioEntry(entry.name(), entry.group(), entry.adapter(),
                        entry.options(), entry.scenarioClass()));
            }
            LDLib2.LOGGER.info("[mptest] role '{}' participates in {} scenario(s): {}",
                    mpSession.role(), queue.size(),
                    queue.stream().map(ScenarioEntry::name).toList());
            return;
        }
        var registry = LDLib2Registries.UI_SCENARIOS;
        if (registry == null) {
            LDLib2.LOGGER.error("[uitest] the ldlib2:ui_scenario registry is unavailable");
            return;
        }
        var selected = new ArrayList<ScenarioEntry>();
        for (var holder : registry) {
            UIScenario scenario;
            try {
                scenario = holder.value().get();
            } catch (Throwable t) {
                LDLib2.LOGGER.error("[uitest] could not instantiate scenario '{}'",
                        holder.annotation().name(), t);
                continue;
            }
            var options = new ScenarioOptions();
            try {
                scenario.configure(options);
            } catch (Throwable t) {
                LDLib2.LOGGER.error("[uitest] scenario '{}' threw while configuring",
                        holder.annotation().name(), t);
                continue;
            }
            if (!config.matches(holder.annotation(), scenario.getClass(), options)) continue;
            selected.add(new ScenarioEntry(holder.annotation().name(), holder.annotation().group(),
                    scenario, options, scenario.getClass()));
        }
        // Registry iteration order is priority-based and stable, but sorting by name makes a run's
        // output diffable against another run regardless of what got registered in between.
        selected.sort(java.util.Comparator.comparing(ScenarioEntry::name));
        queue.addAll(selected);
        LDLib2.LOGGER.info("[uitest] selected {} scenario(s): {}", selected.size(),
                selected.stream().map(ScenarioEntry::name).toList());
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]+", "_");
    }
}
