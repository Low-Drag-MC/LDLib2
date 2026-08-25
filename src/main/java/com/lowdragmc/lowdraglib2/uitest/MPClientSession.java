package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.uitest.mp.MPHubClient;
import com.lowdragmc.lowdraglib2.uitest.mp.MPMessages;
import com.lowdragmc.lowdraglib2.uitest.mp.MPRunConfig;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenario;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.mp.MPSegment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * One client's side of a multi-process run: the hub connection, and the compiler that turns a
 * {@link MPScenario}'s segment list into an ordinary scenario the unchanged {@link UITestRunner}
 * step machine can execute.
 *
 * <p>The compilation rule per segment: a segment this client owns expands into its real steps plus
 * an announce step; a segment someone else owns becomes a single wait step polling the hub mirror.
 * That is the entire cross-process protocol from the runner's point of view — everything else
 * (screenshots, reports, timeouts, teardown, the watchdog) is the existing machinery.
 */
final class MPClientSession {

    /** Cross-process sync waits span server ticks plus two network hops; 5 s solo default is too tight. */
    private static final long DEFAULT_SYNC_TIMEOUT_MS = 30_000;

    private final MPRunConfig config;
    private final MPHubClient hub = new MPHubClient();
    private final Map<String, Compiled> compiledByName = new LinkedHashMap<>();
    private int connectAttempts;

    private static final class Compiled {
        final String name;
        final Class<?> scenarioClass;
        final MPScenarioOptions mpOptions;
        final List<MPSegment> segments;
        final List<MPSegment> teardown;
        final Set<Integer> announced = new HashSet<>();
        /** Segment index → in-flight probe id, for sync polling. */
        final Map<Integer, String> pendingProbes = new HashMap<>();

        Compiled(String name, Class<?> scenarioClass, MPScenarioOptions mpOptions,
                 List<MPSegment> segments, List<MPSegment> teardown) {
            this.name = name;
            this.scenarioClass = scenarioClass;
            this.mpOptions = mpOptions;
            this.segments = segments;
            this.teardown = teardown;
        }
    }

    record Entry(String name, String group, UIScenario adapter, ScenarioOptions options, Class<?> scenarioClass) {
    }

    MPClientSession(MPRunConfig config) {
        this.config = config;
    }

    String role() {
        return config.role();
    }

    MPHubClient hub() {
        return hub;
    }

    /** Off-thread: the render thread must never block on a socket. */
    void connectAsync() {
        var thread = new Thread(() -> {
            try {
                hub.connect(config.hubHost(), config.hubPort(), config.role());
            } catch (IOException e) {
                LDLib2.LOGGER.error("[mptest] cannot reach the control hub at {}:{}",
                        config.hubHost(), config.hubPort(), e);
                hub.markLost();
            }
        }, "ldlib2-mptest-connect");
        thread.setDaemon(true);
        thread.start();
    }

    int gamePort() {
        var hubConfig = hub.config();
        return hubConfig == null || hubConfig.gamePort == null ? -1 : hubConfig.gamePort;
    }

    void beginConnectAttempt() {
        connectAttempts++;
    }

    int connectAttempts() {
        return connectAttempts;
    }

    void sendJoined() {
        hub.sendJoined(config.role());
    }

    // region compilation

    /**
     * Selects, defines and compiles the scenarios this client takes part in. Every process runs the
     * same selection over the same sorted registry, so the resulting scenario order is identical
     * everywhere — which is what lets barrier state be keyed by plain (scenario, segment, role).
     */
    List<Entry> compileSelection() {
        var registry = LDLib2Registries.MP_SCENARIOS;
        var hubConfig = hub.config();
        if (registry == null || hubConfig == null) {
            LDLib2.LOGGER.error("[mptest] cannot compile scenarios: registry {} / hub config {}",
                    registry == null ? "missing" : "ok", hubConfig == null ? "missing" : "ok");
            return List.of();
        }
        var selection = hubConfig.selection == null ? "all" : hubConfig.selection;
        var launched = hubConfig.clientRoles == null ? List.<String>of() : hubConfig.clientRoles;

        var holders = new ArrayList<>(registry.values());
        holders.sort(Comparator.comparing(holder -> holder.annotation().name()));

        var entries = new ArrayList<Entry>();
        for (var holder : holders) {
            var name = holder.annotation().name();
            MPScenario scenario;
            var mpOptions = new MPScenarioOptions();
            try {
                scenario = holder.value().get();
                scenario.configure(mpOptions);
            } catch (Throwable t) {
                LDLib2.LOGGER.error("[mptest] could not instantiate/configure MP scenario '{}'", name, t);
                continue;
            }
            if (!RunConfig.selectionMatches(selection, name, holder.annotation().group(),
                    mpOptions.tags(), scenario.getClass())) {
                continue;
            }
            if (!launched.containsAll(mpOptions.clients())) {
                LDLib2.LOGGER.warn("[mptest] skipping '{}': it needs clients {} but this run launched {}",
                        name, mpOptions.clients(), launched);
                continue;
            }
            if (!mpOptions.clients().contains(role())) {
                LDLib2.LOGGER.info("[mptest] '{}' does not involve role '{}'", name, role());
                continue;
            }
            var builder = new MPScenarioBuilder();
            try {
                scenario.define(builder);
            } catch (Throwable t) {
                LDLib2.LOGGER.error("[mptest] MP scenario '{}' failed while being defined", name, t);
                continue;
            }
            var compiled = new Compiled(name, scenario.getClass(), mpOptions,
                    builder.segments(), builder.teardownSegments());
            compiledByName.put(name, compiled);

            var options = new ScenarioOptions();
            options.scenarioTimeoutMs(mpOptions.scenarioTimeoutMs());
            options.tags(mpOptions.tags().toArray(String[]::new));
            mpOptions.clientOptions().accept(options);

            UIScenario adapter = scenarioBuilder -> expand(compiled, scenarioBuilder);
            entries.add(new Entry(name, holder.annotation().group(), adapter, options, scenario.getClass()));
        }
        return entries;
    }

    private void expand(Compiled compiled, ScenarioBuilder b) {
        for (var segment : compiled.segments) {
            var reporters = reportersOf(compiled, segment);
            if (segment.ownedByClient(role())) {
                switch (segment.kind) {
                    case CLIENT, ALL_CLIENTS -> b.group("segment " + segment.index + ": " + segment.name,
                            Objects.requireNonNull(segment.clientBlock));
                    case SYNC -> b.timeoutMs(segment.timeoutMs >= 0 ? segment.timeoutMs : DEFAULT_SYNC_TIMEOUT_MS)
                            .step("sync: " + segment.name, ctx -> pollSync(ctx, compiled, segment));
                    case FETCH -> b.timeoutMs(segment.timeoutMs >= 0 ? segment.timeoutMs : DEFAULT_SYNC_TIMEOUT_MS)
                            .step("fetch: " + segment.name, ctx -> pollFetch(ctx, compiled, segment));
                    default -> throw new IllegalStateException("client-owned " + segment.kind);
                }
                b.step("announce segment " + segment.index, ctx -> announce(compiled, segment));
                // An ALL_CLIENTS or all-role SYNC segment is only over when the other clients are
                // done too; solo-owned segments have no one else to wait for.
                if (reporters.size() > 1) {
                    addBarrier(b, compiled, segment, reporters);
                }
            } else {
                addBarrier(b, compiled, segment, reporters);
            }
        }
        for (var segment : compiled.teardown) {
            if (!segment.ownedByClient(role())) continue;
            if (segment.clientBlock == null) {
                throw new IllegalStateException("teardown segment " + segment + " has no client block");
            }
            // Teardown runs without barriers — each process cleans up what it owns, best effort —
            // but the block still expands into individual steps so each gets its own frame.
            var sub = new ScenarioBuilder();
            segment.clientBlock.accept(sub);
            for (var step : sub.steps()) {
                b.teardown(step.name, ctx -> {
                    try {
                        step.body.run(ctx);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private void addBarrier(ScenarioBuilder b, Compiled compiled, MPSegment segment, Set<String> reporters) {
        // The barrier's budget is the scenario's, not the 5 s step default: it legitimately spans
        // however long another process's whole segment takes.
        b.timeoutMs(compiled.mpOptions.scenarioTimeoutMs())
                .step("barrier: segment " + segment.index + " '" + segment.name + "'",
                        ctx -> awaitSegment(ctx, compiled, segment, reporters));
    }

    private Set<String> reportersOf(Compiled compiled, MPSegment segment) {
        return segment.reporters(MPMessages.SERVER_ROLE, compiled.mpOptions.clients());
    }

    // endregion

    // region step bodies

    private void awaitSegment(TestContext ctx, Compiled compiled, MPSegment segment, Set<String> reporters) {
        for (var reporter : reporters) {
            var status = hub.segmentStatus(compiled.name, segment.index, reporter);
            if (status == null) {
                if (hub.isRoleDead(reporter)) {
                    throw new IllegalStateException("the '" + reporter
                            + "' process died before finishing segment " + segment);
                }
                ctx.repeat("segment " + segment.index + " '" + segment.name + "' on '" + reporter + "'");
                return;
            }
            if (!MPMessages.DONE.equals(status)) {
                throw new IllegalStateException("segment " + segment + " ended " + status
                        + " on '" + reporter + "' - aborting this scenario here too");
            }
        }
    }

    private void pollSync(TestContext ctx, Compiled compiled, MPSegment segment) {
        String clientJson;
        try {
            clientJson = MPMessages.GSON.toJson(Objects.requireNonNull(segment.clientProbe).apply(ctx));
        } catch (Exception e) {
            throw new IllegalStateException("the client-side getter of sync '" + segment.name + "' failed", e);
        }
        var probeId = compiled.pendingProbes.get(segment.index);
        if (probeId == null) {
            compiled.pendingProbes.put(segment.index,
                    hub.sendProbeRequest(role(), compiled.name, segment.index));
            ctx.repeat("the server value for '" + segment.name + "'");
            return;
        }
        var response = hub.probeResponse(probeId);
        if (response == null) {
            if (hub.isRoleDead(MPMessages.SERVER_ROLE)) {
                throw new IllegalStateException("the server process died during sync '" + segment.name + "'");
            }
            ctx.repeat("the server value for '" + segment.name + "'");
            return;
        }
        hub.clearProbeResponse(probeId);
        compiled.pendingProbes.remove(segment.index);
        if (response.error != null) {
            throw new IllegalStateException("the server-side getter of sync '" + segment.name
                    + "' failed: " + response.error);
        }
        if (!Objects.equals(response.valueJson, clientJson)) {
            // A fresh request next attempt, so each comparison is against current server state.
            ctx.repeat(segment.name + " (server=" + response.valueJson + ", client=" + clientJson + ")");
            return;
        }
        ctx.log(segment.name + " settled at " + clientJson);
    }

    /**
     * One probe round trip, stored rather than compared — see {@link MPSegment.Kind#FETCH}.
     *
     * <p>The polling shape is {@code pollSync}'s and has to be: a step body runs once per frame and
     * may not block, so "ask" and "read the answer" are two visits with a {@code repeat} between
     * them.
     */
    private void pollFetch(TestContext ctx, Compiled compiled, MPSegment segment) {
        var probeId = compiled.pendingProbes.get(segment.index);
        if (probeId == null) {
            compiled.pendingProbes.put(segment.index,
                    hub.sendProbeRequest(role(), compiled.name, segment.index));
            ctx.repeat("the server value for '" + segment.name + "'");
            return;
        }
        var response = hub.probeResponse(probeId);
        if (response == null) {
            if (hub.isRoleDead(MPMessages.SERVER_ROLE)) {
                throw new IllegalStateException("the server process died during fetch '" + segment.name + "'");
            }
            ctx.repeat("the server value for '" + segment.name + "'");
            return;
        }
        hub.clearProbeResponse(probeId);
        compiled.pendingProbes.remove(segment.index);
        if (response.error != null) {
            throw new IllegalStateException("the server-side getter of fetch '" + segment.name
                    + "' failed: " + response.error);
        }
        var key = Objects.requireNonNull(segment.stateKey, "a FETCH segment needs a state key");
        var value = MPMessages.GSON.fromJson(response.valueJson,
                Objects.requireNonNull(segment.valueType, "a FETCH segment needs a value type"));
        // Nulls go in too. A key that is simply absent later reads as "the fetch never ran", which
        // is a different fault from "the server had nothing to give", and the two want different fixes.
        ctx.state().put(key, value);
        ctx.log(segment.name + " -> " + key + " = " + response.valueJson);
    }

    private void announce(Compiled compiled, MPSegment segment) {
        compiled.announced.add(segment.index);
        hub.sendSegmentDone(role(), compiled.name, segment.index, MPMessages.DONE);
    }

    // endregion

    // region lifecycle hooks

    /**
     * Called for every scenario end, normal or aborted. Whatever this client never announced —
     * because a step threw, the scenario timed out, or a barrier gave up — is broadcast as ABORTED,
     * so no other process waits out its own timeout on a segment that will never come.
     */
    void onScenarioFinished(String name, String status) {
        var compiled = compiledByName.get(name);
        if (compiled == null) return;
        for (var segment : compiled.segments) {
            if (segment.ownedByClient(role()) && !compiled.announced.contains(segment.index)) {
                hub.sendSegmentDone(role(), name, segment.index, MPMessages.ABORTED);
            }
        }
        hub.sendScenarioDone(role(), name, status);
    }

    void onRunDone(String status) {
        hub.sendRunDone(role(), status);
    }

    // endregion
}
