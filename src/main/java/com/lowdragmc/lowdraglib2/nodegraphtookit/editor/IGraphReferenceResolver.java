package com.lowdragmc.lowdraglib2.nodegraphtookit.editor;

import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves an {@link IResourcePath} to a loaded {@link Graph}. Used by external (asset)
 * subgraph nodes to fetch the inner graph on demand. The editor wires this in when it
 * loads a graph for editing; outside an editor context it stays null and external
 * subgraph nodes fall back to their cached port shape.
 */
@FunctionalInterface
public interface IGraphReferenceResolver {
    @Nullable
    Graph resolve(IResourcePath path);
}
