package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.uitest.mp.MPHubClient;
import com.lowdragmc.lowdraglib2.uitest.mp.MPMessages;
import com.lowdragmc.lowdraglib2.uitest.mp.MPRunConfig;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.mp.MPSegment;
import com.lowdragmc.lowdraglib2.uitest.report.ReportWriter;
import com.lowdragmc.lowdraglib2.uitest.report.RunReport;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The dedicated server's side of a multi-process run: a per-tick state machine that executes the
 * server-owned segments of every selected {@code MPScenario}, waits at barriers for the clients'
 * segments, and services sync probes at any time.
 *
 * <p>Runs entirely on the server thread (driven from {@code ServerTickEvent.Post}), so segment
 * bodies see authoritative state with no extra synchronisation — the same guarantee
 * {@code ScenarioBuilder#server} gives solo scenarios on the integrated server.
 */
final class MPServerRunner {

    private enum State {CONNECTING, AWAIT_BEGIN, RUN, DRAINING, DONE}

    private static final long DEFAULT_SERVER_WAIT_TIMEOUT_MS = 30_000;

    private record ServerCompiled(String name, String group, Class<?> scenarioClass,
                                  MPScenarioOptions options, List<MPSegment> segments,
                                  List<MPSegment> teardown, Set<Integer> announced) {
    }

    private final MPRunConfig config;
    private final MinecraftServer server;
    private final MPHubClient hub = new MPHubClient();
    private final RunReport report = new RunReport();
    private final long startedNanos = System.nanoTime();

    private volatile State state = State.CONNECTING;
    private long drainStartedMs;
    private List<ServerCompiled> scenarios;
    private final Map<String, ServerCompiled> byName = new LinkedHashMap<>();

    // per-scenario cursor state
    private int scenarioIndex;
    private int cursor;
    private long scenarioStartedMs;
    private long segmentStartedMs;
    private boolean scenarioAborted;
    private RunReport.ScenarioReport currentReport;
    private Map<String, Object> scratch = new ConcurrentHashMap<>();

    MPServerRunner(MPRunConfig config, MinecraftServer server) {
        this.config = config;
        this.server = server;
    }

    /** Called once from {@code ServerStartedEvent}, on the server thread. */
    void start() {
        report.runId = "mp_" + MPMessages.SERVER_ROLE;
        report.startedAt = System.currentTimeMillis();
        report.environment.java = System.getProperty("java.version", "");
        report.environment.os = System.getProperty("os.name", "") + " " + System.getProperty("os.version", "");
        WorldRules.pin(server);
        // Off-thread: connect() blocks, and a hub that is slow to accept must not stall ticks.
        var thread = new Thread(() -> {
            try {
                hub.connect(config.hubHost(), config.hubPort(), MPMessages.SERVER_ROLE);
                hub.sendServerReady(server.getPort());
                state = State.AWAIT_BEGIN;
            } catch (IOException e) {
                LDLib2.LOGGER.error("[mptest] server cannot reach the control hub at {}:{}",
                        config.hubHost(), config.hubPort(), e);
                hub.markLost();
            }
        }, "ldlib2-mptest-connect");
        thread.setDaemon(true);
        thread.start();
        LDLib2.LOGGER.info("[mptest] dedicated server armed, game port {}", server.getPort());
    }

    /** Called every {@code ServerTickEvent.Post}. */
    void tick() {
        if (state == State.DONE) return;
        if (hub.isLost() && state != State.DRAINING) {
            LDLib2.LOGGER.error("[mptest] control hub gone - writing the report and stopping");
            finishRun();
        }
        switch (state) {
            case CONNECTING -> {
                if (elapsedMs() > 60_000) {
                    LDLib2.LOGGER.error("[mptest] hub connection never completed");
                    finishRun();
                }
            }
            case AWAIT_BEGIN -> {
                if (hub.isBegun()) {
                    compileScenarios();
                    LDLib2.LOGGER.info("[mptest] server starting {} scenario(s): {}",
                            scenarios.size(), scenarios.stream().map(ServerCompiled::name).toList());
                    state = State.RUN;
                } else if (elapsedMs() > 420_000) {
                    LDLib2.LOGGER.error("[mptest] 'begin' never arrived - did a client fail to join?");
                    finishRun();
                }
            }
            case RUN -> runTick();
            case DRAINING -> {
                // Stopping the server the moment its own scenarios end would sever clients that are
                // still in teardown - their close-screen steps then die on "Trying to return to
                // in-game GUI during disconnection". Outlive them instead.
                boolean allFinished = clientRoles().stream().allMatch(hub::isRoleFinished);
                if (allFinished || hub.isLost() || System.currentTimeMillis() - drainStartedMs > 60_000) {
                    state = State.DONE;
                    LDLib2.LOGGER.info("[mptest] stopping the dedicated server");
                    server.halt(false);
                }
            }
            case DONE -> {
            }
        }
    }

    private List<String> clientRoles() {
        var hubConfig = hub.config();
        return hubConfig == null || hubConfig.clientRoles == null ? List.of() : hubConfig.clientRoles;
    }

    void close() {
        hub.close();
    }

    // region scenario selection

    private void compileScenarios() {
        scenarios = new ArrayList<>();
        var registry = LDLib2Registries.MP_SCENARIOS;
        var hubConfig = hub.config();
        if (registry == null || hubConfig == null) {
            LDLib2.LOGGER.error("[mptest] cannot compile scenarios on the server: registry {} / config {}",
                    registry == null ? "missing" : "ok", hubConfig == null ? "missing" : "ok");
            return;
        }
        report.selection = hubConfig.selection == null ? "all" : hubConfig.selection;
        var launched = hubConfig.clientRoles == null ? List.<String>of() : hubConfig.clientRoles;

        var holders = new ArrayList<>(registry.values());
        holders.sort(Comparator.comparing(holder -> holder.annotation().name()));
        for (var holder : holders) {
            var name = holder.annotation().name();
            try {
                var scenario = holder.value().get();
                var options = new MPScenarioOptions();
                scenario.configure(options);
                if (!RunConfig.selectionMatches(report.selection, name, holder.annotation().group(),
                        options.tags(), scenario.getClass())) {
                    continue;
                }
                if (!launched.containsAll(options.clients())) {
                    LDLib2.LOGGER.warn("[mptest] skipping '{}': it needs clients {} but this run launched {}",
                            name, options.clients(), launched);
                    continue;
                }
                var builder = new MPScenarioBuilder();
                scenario.define(builder);
                var compiled = new ServerCompiled(name, holder.annotation().group(), scenario.getClass(),
                        options, builder.segments(), builder.teardownSegments(), new HashSet<>());
                scenarios.add(compiled);
                byName.put(name, compiled);
            } catch (Throwable t) {
                LDLib2.LOGGER.error("[mptest] MP scenario '{}' failed to define on the server", name, t);
            }
        }
    }

    // endregion

    // region run loop

    private void runTick() {
        serviceProbes();
        if (scenarios == null) return;
        // Advance as far as this tick allows; an unmet barrier or an unsatisfied server wait yields.
        int guard = 0;
        while (state == State.RUN && guard++ < 1000) {
            if (scenarioIndex >= scenarios.size()) {
                finishRun();
                return;
            }
            var compiled = scenarios.get(scenarioIndex);
            if (currentReport == null) beginScenario(compiled);
            if (!scenarioAborted && System.currentTimeMillis() - scenarioStartedMs > compiled.options().scenarioTimeoutMs()) {
                abortScenario(compiled, "scenario exceeded its " + compiled.options().scenarioTimeoutMs() + " ms budget");
            }
            if (scenarioAborted || cursor >= compiled.segments().size()) {
                endScenario(compiled);
                continue;
            }
            var segment = compiled.segments().get(cursor);
            var advanced = segment.serverOwned()
                    ? executeOwned(compiled, segment)
                    : checkForeign(compiled, segment);
            if (!advanced) return;
        }
    }

    private void beginScenario(ServerCompiled compiled) {
        currentReport = new RunReport.ScenarioReport();
        currentReport.name = compiled.name();
        currentReport.group = compiled.group();
        currentReport.className = compiled.scenarioClass().getName();
        currentReport.tags.addAll(compiled.options().tags());
        report.scenarios.add(currentReport);
        scenarioStartedMs = System.currentTimeMillis();
        segmentStartedMs = 0;
        scratch = new ConcurrentHashMap<>();
        LDLib2.LOGGER.info("[mptest] --- scenario '{}' ({} segments)", compiled.name(), compiled.segments().size());
    }

    /** @return whether the cursor advanced (false = still waiting, yield until next tick) */
    private boolean executeOwned(ServerCompiled compiled, MPSegment segment) {
        if (segment.kind == MPSegment.Kind.SERVER) {
            var stepReport = newStepReport(segment);
            try {
                Objects.requireNonNull(segment.serverBody).accept(new ServerContext(server, scratch, stepReport));
                completeStep(stepReport, null);
                announce(compiled, segment, MPMessages.DONE);
            } catch (Throwable t) {
                completeStep(stepReport, t);
                announce(compiled, segment, MPMessages.ERROR);
                abortScenario(compiled, "segment " + segment + " threw: " + t);
            }
            cursor++;
            segmentStartedMs = 0;
            return true;
        }
        // SERVER_WAIT: re-evaluated once per tick until true or timed out.
        if (segmentStartedMs == 0) segmentStartedMs = System.currentTimeMillis();
        var stepReport = currentSegmentStepReport(segment);
        boolean satisfied;
        try {
            satisfied = Objects.requireNonNull(segment.serverCondition)
                    .test(new ServerContext(server, scratch, stepReport));
        } catch (Throwable t) {
            completeStep(stepReport, t);
            announce(compiled, segment, MPMessages.ERROR);
            abortScenario(compiled, "segment " + segment + " threw: " + t);
            cursor++;
            segmentStartedMs = 0;
            return true;
        }
        if (satisfied) {
            completeStep(stepReport, null);
            announce(compiled, segment, MPMessages.DONE);
            cursor++;
            segmentStartedMs = 0;
            return true;
        }
        long timeout = segment.timeoutMs >= 0 ? segment.timeoutMs : DEFAULT_SERVER_WAIT_TIMEOUT_MS;
        if (System.currentTimeMillis() - segmentStartedMs > timeout) {
            completeStep(stepReport, new IllegalStateException(
                    "timed out after " + timeout + " ms waiting for: " + segment.name));
            announce(compiled, segment, MPMessages.ERROR);
            abortScenario(compiled, "segment " + segment + " timed out");
            cursor++;
            segmentStartedMs = 0;
            return true;
        }
        return false;
    }

    /** @return whether the cursor advanced */
    private boolean checkForeign(ServerCompiled compiled, MPSegment segment) {
        for (var reporter : reportersOf(compiled, segment)) {
            var status = hub.segmentStatus(compiled.name(), segment.index, reporter);
            if (status == null) {
                if (hub.isRoleDead(reporter)) {
                    abortScenario(compiled, "the '" + reporter + "' process died before finishing segment " + segment);
                    return true;
                }
                return false;
            }
            if (!MPMessages.DONE.equals(status)) {
                abortScenario(compiled, "segment " + segment + " ended " + status + " on '" + reporter + "'");
                return true;
            }
        }
        cursor++;
        return true;
    }

    private void abortScenario(ServerCompiled compiled, String reason) {
        if (scenarioAborted) return;
        scenarioAborted = true;
        LDLib2.LOGGER.error("[mptest] aborting scenario '{}': {}", compiled.name(), reason);
        currentReport.status = RunReport.Status.worst(currentReport.status, RunReport.Status.ERROR);
        if (currentReport.error == null) {
            currentReport.error = RunReport.ErrorInfo.of(new IllegalStateException(reason));
        }
    }

    private void endScenario(ServerCompiled compiled) {
        // Whatever this side never announced fails fast on everyone else's barriers.
        for (var segment : compiled.segments()) {
            if (segment.serverOwned() && !compiled.announced().contains(segment.index)) {
                hub.sendSegmentDone(MPMessages.SERVER_ROLE, compiled.name(), segment.index, MPMessages.ABORTED);
            }
        }
        // Teardown: owned segments only, best effort, no barriers.
        for (var segment : compiled.teardown()) {
            if (!segment.serverOwned()) continue;
            var stepReport = newStepReport(segment);
            try {
                if (segment.serverBody != null) {
                    segment.serverBody.accept(new ServerContext(server, scratch, stepReport));
                } else if (segment.serverCondition != null) {
                    segment.serverCondition.test(new ServerContext(server, scratch, stepReport));
                }
                completeStep(stepReport, null);
            } catch (Throwable t) {
                completeStep(stepReport, t);
            }
        }
        currentReport.durationMs = System.currentTimeMillis() - scenarioStartedMs;
        hub.sendScenarioDone(MPMessages.SERVER_ROLE, compiled.name(), currentReport.status);
        LDLib2.LOGGER.info("[mptest] --- scenario '{}' {} on the server in {} ms",
                compiled.name(), currentReport.status, currentReport.durationMs);
        currentReport = null;
        cursor = 0;
        scenarioAborted = false;
        scenarioIndex++;
    }

    private void finishRun() {
        if (state == State.DRAINING || state == State.DONE) return;
        report.finishedAt = System.currentTimeMillis();
        report.durationMs = (System.nanoTime() - startedNanos) / 1_000_000L;
        ReportWriter.finalise(report);
        ReportWriter.write(report, config.outDir());
        ReportWriter.logSummary(report);
        hub.sendRunDone(MPMessages.SERVER_ROLE, report.status);
        LDLib2.LOGGER.info("[mptest] server run {} - waiting for the clients to finish before stopping",
                report.status);
        drainStartedMs = System.currentTimeMillis();
        state = State.DRAINING;
    }

    // endregion

    // region probes

    /**
     * Sync probes are serviced whenever they arrive, not only at this side's own segments: the
     * requesting client is inside <em>its</em> segment while the server is parked on a barrier.
     */
    private void serviceProbes() {
        MPMessages.Msg request;
        while ((request = hub.pollProbeRequest()) != null) {
            try {
                var compiled = byName.get(request.scenario);
                if (compiled == null || request.segment == null
                        || request.segment < 0 || request.segment >= compiled.segments().size()) {
                    hub.sendProbeResponse(request.probeId, null, "unknown probe target " + request.scenario
                            + "#" + request.segment);
                    continue;
                }
                var segment = compiled.segments().get(request.segment);
                if (segment.serverProbe == null) {
                    hub.sendProbeResponse(request.probeId, null, "segment " + segment + " has no server probe");
                    continue;
                }
                // A throwaway report: probes are getters, their checks are not recorded anywhere.
                var value = segment.serverProbe.apply(new ServerContext(server, scratch, new RunReport.StepReport()));
                hub.sendProbeResponse(request.probeId, MPMessages.GSON.toJson(value), null);
            } catch (Throwable t) {
                hub.sendProbeResponse(request.probeId, null, String.valueOf(t));
            }
        }
    }

    // endregion

    // region reporting plumbing

    private RunReport.StepReport newStepReport(MPSegment segment) {
        var stepReport = new RunReport.StepReport();
        stepReport.index = segment.index + 1;
        stepReport.name = segment.name;
        stepReport.kind = segment.kind.name();
        stepReport.role = MPMessages.SERVER_ROLE;
        currentReport.steps.add(stepReport);
        return stepReport;
    }

    /** The lazily-created report for a SERVER_WAIT segment, shared across its re-evaluations. */
    private RunReport.StepReport currentSegmentStepReport(MPSegment segment) {
        if (!currentReport.steps.isEmpty()) {
            var last = currentReport.steps.getLast();
            if (last.index == segment.index + 1) return last;
        }
        return newStepReport(segment);
    }

    private void completeStep(RunReport.StepReport stepReport, Throwable failure) {
        boolean checksFailed = stepReport.checks.stream().anyMatch(check -> !check.passed);
        if (failure != null) {
            stepReport.status = RunReport.Status.ERROR;
            stepReport.error = RunReport.ErrorInfo.of(failure);
            LDLib2.LOGGER.error("[mptest] server segment '{}' errored", stepReport.name, failure);
        } else if (checksFailed) {
            stepReport.status = RunReport.Status.FAIL;
        } else {
            stepReport.status = RunReport.Status.PASS;
        }
        currentReport.status = RunReport.Status.worst(currentReport.status, stepReport.status);
    }

    private void announce(ServerCompiled compiled, MPSegment segment, String status) {
        compiled.announced().add(segment.index);
        hub.sendSegmentDone(MPMessages.SERVER_ROLE, compiled.name(), segment.index, status);
    }

    private Set<String> reportersOf(ServerCompiled compiled, MPSegment segment) {
        return segment.reporters(MPMessages.SERVER_ROLE, compiled.options().clients());
    }

    private long elapsedMs() {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    // endregion
}
