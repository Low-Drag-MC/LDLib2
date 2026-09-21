package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphViewPreferences;
import com.lowdragmc.lowdraglib2.uitest.TestContext;

import java.nio.file.Files;

/**
 * Keeps a scenario's graph-view preferences to itself.
 *
 * <p>{@link GraphViewPreferences} is per graph type and shared by the whole process, which is
 * exactly right in a real session and exactly wrong across scenarios: they all edit a
 * {@code TestGraph}, so one scenario switching the wire style would decide what the next one's
 * "default" is, and the two only ever run in that order on some shards. Left alone, that is a
 * failure that appears and disappears with {@code -PldTestJobs}.</p>
 *
 * <p>It also keeps the run out of the real {@code config/ldlib2/graph_view.json}.</p>
 */
final class ScenarioPreferences {

    private ScenarioPreferences() {
    }

    /**
     * Points the store at a scratch file of this scenario's own, starting empty.
     *
     * <p>Call from the UI builder, not from a step: the graph is loaded while the UI is being
     * built, and loading is what reads the remembered setup.</p>
     */
    static void isolate(TestContext ctx, String scenario) {
        var path = ctx.outDir().resolve("graph_view_" + scenario + ".json");
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            LDLib2.LOGGER.warn("Could not clear the scratch preference file {}", path, e);
        }
        GraphViewPreferences.setFile(path);
    }

    /** Hands the store back to the real config file. */
    static void restore() {
        GraphViewPreferences.setFile(null);
    }
}
