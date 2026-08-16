package com.lowdragmc.lowdraglib2.uitest;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Per-scenario knobs, set from {@link UIScenario#configure(ScenarioOptions)}.
 */
public final class ScenarioOptions {

    /**
     * Idle time after each step.
     *
     * <p>Wall-clock, never a frame count. A dev world renders at several hundred frames a second, so
     * a frame budget that reads as generous is a few milliseconds in practice — the UI has not
     * rebuilt before the next step looks at it, and captures silently show the previous state.
     *
     * <p>The default is one client tick plus a margin. {@code ModularUI#tick()} runs at 20 Hz off
     * the client tick, and that is what refreshes data-bound labels, so anything shorter would make
     * assertions on synced values racy.
     */
    private long defaultSettleMs = 60;
    private long defaultTimeoutMs = 5_000;
    private long scenarioTimeoutMs = 120_000;
    private ErrorPolicy errorPolicy = ErrorPolicy.ABORT_SCENARIO;
    private boolean captureOnFailure = true;
    private boolean captureEveryStep = false;
    private boolean requiresWorld = true;
    private int guiScale = -1;
    private final Set<String> tags = new LinkedHashSet<>();

    public ScenarioOptions defaultSettleMs(long ms) {
        this.defaultSettleMs = Math.max(0, ms);
        return this;
    }

    public ScenarioOptions defaultTimeoutMs(long ms) {
        this.defaultTimeoutMs = Math.max(1, ms);
        return this;
    }

    public ScenarioOptions scenarioTimeoutMs(long ms) {
        this.scenarioTimeoutMs = Math.max(1, ms);
        return this;
    }

    public ScenarioOptions onError(ErrorPolicy policy) {
        this.errorPolicy = policy;
        return this;
    }

    /** Grab a screenshot automatically whenever a step fails or throws. On by default. */
    public ScenarioOptions captureOnFailure(boolean capture) {
        this.captureOnFailure = capture;
        return this;
    }

    /** Grab a screenshot after every step. Expensive; useful when bisecting a flaky scenario. */
    public ScenarioOptions captureEveryStep(boolean capture) {
        this.captureEveryStep = capture;
        return this;
    }

    /**
     * Whether the scenario needs a loaded world. Pure title-screen or resource tests can turn this
     * off and skip world creation entirely.
     */
    public ScenarioOptions requiresWorld(boolean requiresWorld) {
        this.requiresWorld = requiresWorld;
        return this;
    }

    /**
     * GUI scale for this scenario, overriding the run's. {@code -1} inherits; {@code 0} is auto.
     *
     * <p>Worth setting per scenario because the right value depends entirely on the UI under test. A
     * small panel is illegible in a screenshot at scale 2 on a big monitor and wants 4; a full-screen
     * editor at scale 8 gets a 480x257 logical viewport and its layout simply collapses. There is no
     * single value that suits both, so the scenario that knows the UI picks.
     */
    public ScenarioOptions guiScale(int guiScale) {
        this.guiScale = guiScale;
        return this;
    }

    public int guiScaleOverride() {
        return guiScale;
    }

    /** Free-form labels, selectable from the command line with {@code -PldTest=tag:<tag>}. */
    public ScenarioOptions tags(String... tags) {
        this.tags.addAll(java.util.Arrays.asList(tags));
        return this;
    }

    public long defaultSettleMs() {
        return defaultSettleMs;
    }

    public long defaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    public long scenarioTimeoutMs() {
        return scenarioTimeoutMs;
    }

    public ErrorPolicy errorPolicy() {
        return errorPolicy;
    }

    public boolean captureOnFailure() {
        return captureOnFailure;
    }

    public boolean captureEveryStep() {
        return captureEveryStep;
    }

    public boolean requiresWorld() {
        return requiresWorld;
    }

    public Set<String> tags() {
        return tags;
    }
}
