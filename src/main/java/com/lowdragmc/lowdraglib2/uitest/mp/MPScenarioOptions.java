package com.lowdragmc.lowdraglib2.uitest.mp;

import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Per-scenario knobs for a {@link MPScenario}, set from {@link MPScenario#configure}.
 *
 * <p>Evaluated in every process, so everything here must be deterministic.
 */
public final class MPScenarioOptions {

    private final List<String> clients = new ArrayList<>(List.of("A"));
    /**
     * Generous by default: a multi-process scenario spends real wall clock waiting on other
     * processes' segments, and each client segment internally has the solo default budgets.
     */
    private long scenarioTimeoutMs = 300_000;
    private final Set<String> tags = new LinkedHashSet<>();
    private Consumer<ScenarioOptions> clientOptions = options -> {
    };

    /**
     * The client roles this scenario needs, e.g. {@code clients("A", "B")}. A run launched with
     * fewer clients than a scenario asks for skips that scenario; extra clients just wait along.
     */
    public MPScenarioOptions clients(String... roles) {
        clients.clear();
        clients.addAll(List.of(roles));
        return this;
    }

    public MPScenarioOptions scenarioTimeoutMs(long ms) {
        this.scenarioTimeoutMs = Math.max(1, ms);
        return this;
    }

    /** Free-form labels, selectable with {@code -PldMpTest=tag:<tag>}. */
    public MPScenarioOptions tags(String... tags) {
        this.tags.addAll(List.of(tags));
        return this;
    }

    /**
     * Adjusts the per-client {@link ScenarioOptions} (gui scale, settle, capture policy) applied
     * when each client compiles its segments into an ordinary scenario run.
     */
    public MPScenarioOptions clientOptions(Consumer<ScenarioOptions> configure) {
        this.clientOptions = this.clientOptions.andThen(configure);
        return this;
    }

    public List<String> clients() {
        return clients;
    }

    public long scenarioTimeoutMs() {
        return scenarioTimeoutMs;
    }

    public Set<String> tags() {
        return tags;
    }

    public Consumer<ScenarioOptions> clientOptions() {
        return clientOptions;
    }
}
