package com.lowdragmc.lowdraglib2.nodegraphtookit.editor;

import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;

import java.util.HashSet;
import java.util.Set;

/**
 * Central bus for "an external subgraph asset was just saved" events.
 *
 * <p>Two flavors of subscribers:</p>
 * <ul>
 *   <li><b>{@link GraphModel} (root)</b> — used by tests and headless contexts; on notify, the
 *       root graph (and its nested local subgraphs) re-define any {@code SubgraphNodeModel} that
 *       references the saved path. Pure model-layer behavior, no UI dependency.</li>
 *   <li><b>{@link Listener}</b> — used by the editor UI. Lets the editor view decide what to do
 *       (refresh ports <em>and</em> potentially reload its root if the saved path matches
 *       what it's currently editing).</li>
 * </ul>
 *
 * <p>Reference-equality tracked; subscribers must explicitly {@code unregister} when they go
 * away.</p>
 */
public final class SubgraphRegistry {
    public static final SubgraphRegistry INSTANCE = new SubgraphRegistry();

    /** Subscriber that gets called when an external graph at {@code path} is saved. */
    @FunctionalInterface
    public interface Listener {
        void onExternalGraphSaved(IResourcePath path);
    }

    private final Set<GraphModel> rootGraphs = new HashSet<>();
    private final Set<Listener> listeners = new HashSet<>();

    private SubgraphRegistry() {}

    // ------------ GraphModel mode (model-only, no UI) ------------

    public synchronized void register(GraphModel rootGraph) {
        if (rootGraph != null) rootGraphs.add(rootGraph);
    }

    public synchronized void unregister(GraphModel rootGraph) {
        if (rootGraph != null) rootGraphs.remove(rootGraph);
    }

    // ------------ Listener mode (UI / custom hooks) ------------

    public synchronized void registerListener(Listener listener) {
        if (listener != null) listeners.add(listener);
    }

    public synchronized void unregisterListener(Listener listener) {
        if (listener != null) listeners.remove(listener);
    }

    /**
     * Broadcast: the external graph identified by {@code path} was saved. Snapshots both
     * subscriber sets to avoid CME if a handler mutates the registry.
     */
    public synchronized void notifyExternalGraphSaved(IResourcePath path) {
        if (path == null) return;
        for (var root : new HashSet<>(rootGraphs)) {
            try {
                root.redefineSubgraphNodeModelsByPath(path);
            } catch (Throwable ignored) {
                // one bad subscriber must not stop the others
            }
        }
        for (var listener : new HashSet<>(listeners)) {
            try {
                listener.onExternalGraphSaved(path);
            } catch (Throwable ignored) {
                // same here
            }
        }
    }
}
