package com.lowdragmc.lowdraglib2.nodegraphtookit.editor;

import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks which open editors hold graphs that may reference a given external subgraph asset,
 * so a "this resource was saved" event can flow back to all the {@code SubgraphNodeModel}s that
 * depend on it. Registered via the root {@link GraphModel} of every open editor — that root is
 * scanned (along with its nested local subgraphs) on broadcast.
 *
 * <p>Roots are reference-equality tracked: an editor must explicitly {@link #unregister} when it
 * closes; this class makes no assumption about GC ordering.</p>
 */
public final class SubgraphRegistry {
    public static final SubgraphRegistry INSTANCE = new SubgraphRegistry();

    private final Set<GraphModel> rootGraphs = new HashSet<>();

    private SubgraphRegistry() {}

    public synchronized void register(GraphModel rootGraph) {
        if (rootGraph != null) rootGraphs.add(rootGraph);
    }

    public synchronized void unregister(GraphModel rootGraph) {
        if (rootGraph != null) rootGraphs.remove(rootGraph);
    }

    /**
     * Broadcast: the external graph identified by {@code path} was saved. Each registered root
     * (and its nested local subgraphs) walks its subgraph nodes; nodes whose external path equals
     * {@code path} drop their cached resolved inner graph and re-define their ports.
     */
    public synchronized void notifyExternalGraphSaved(IResourcePath path) {
        if (path == null) return;
        // snapshot to avoid CME if a handler unregisters
        for (var root : new HashSet<>(rootGraphs)) {
            try {
                root.redefineSubgraphNodeModelsByPath(path);
            } catch (Throwable ignored) {
                // a broken handler in one editor must not stop the others
            }
        }
    }
}
