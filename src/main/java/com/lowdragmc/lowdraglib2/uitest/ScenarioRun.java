package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.uitest.report.RunReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The live state of one scenario: its step queue and the step machine's cursor.
 *
 * <p>Kept separate from the runner so that "where are we in this scenario" and "what phase is the
 * whole run in" stay independent — a scenario aborting is normal and must not disturb the run.
 */
public final class ScenarioRun {

    public final String name;
    public final ScenarioOptions options;
    public final List<Step> steps;
    public final List<Step> teardownSteps;
    public final RunReport.ScenarioReport report;

    /** Shared with {@link ServerContext}; concurrent because the server thread writes to it. */
    public final Map<String, Object> state = new ConcurrentHashMap<>();

    // step machine
    private final List<Step> executionQueue = new ArrayList<>();
    private int cursor;
    private Step current;
    private int attempt;
    private long firstAttemptAtNanos;
    private long startedAtNanos;
    private boolean teardownStarted;

    /** Set by {@link TestContext#repeat(String)}; cleared before each attempt. */
    boolean repeatRequested;
    String waitingFor;

    public ScenarioRun(String name, ScenarioOptions options, List<Step> steps, List<Step> teardownSteps,
                       RunReport.ScenarioReport report) {
        this.name = name;
        this.options = options;
        this.steps = steps;
        this.teardownSteps = teardownSteps;
        this.report = report;
        this.executionQueue.addAll(steps);
    }

    public void begin(long nowNanos) {
        startedAtNanos = nowNanos;
    }

    public long elapsedNanos(long nowNanos) {
        return nowNanos - startedAtNanos;
    }

    /**
     * The step to run now, or {@code null} when the scenario is finished.
     *
     * <p>Reaching the end of the main queue switches to the teardown queue rather than finishing, so
     * cleanup runs exactly once whether the scenario completed, failed or aborted.
     */
    public Step peek() {
        if (cursor < executionQueue.size()) {
            return executionQueue.get(cursor);
        }
        if (!teardownStarted) {
            teardownStarted = true;
            cursor = 0;
            executionQueue.clear();
            executionQueue.addAll(teardownSteps);
            return executionQueue.isEmpty() ? null : executionQueue.getFirst();
        }
        return null;
    }

    public void advance() {
        cursor++;
        current = null;
        attempt = 0;
    }

    /**
     * Skips the rest of the scenario but keeps teardown. Called when a step throws under
     * {@link ErrorPolicy#ABORT_SCENARIO}, and when the scenario as a whole times out.
     */
    public void abortToTeardown() {
        if (!teardownStarted) {
            cursor = executionQueue.size();
        }
    }

    public boolean isInTeardown() {
        return teardownStarted;
    }

    public Step current() {
        return current;
    }

    public void startAttempt(Step step, long nowNanos) {
        if (current != step) {
            current = step;
            attempt = 0;
            firstAttemptAtNanos = nowNanos;
        }
        attempt++;
        repeatRequested = false;
    }

    public int attempt() {
        return attempt;
    }

    public long waitedNanos(long nowNanos) {
        return nowNanos - firstAttemptAtNanos;
    }

    public long effectiveSettleMs(Step step) {
        return step.settleMs >= 0 ? step.settleMs : options.defaultSettleMs();
    }

    public long effectiveTimeoutMs(Step step) {
        return step.timeoutMs >= 0 ? step.timeoutMs : options.defaultTimeoutMs();
    }

    /** 1-based position for report indices and screenshot file-name prefixes. */
    public int stepIndex() {
        return cursor + 1;
    }
}
