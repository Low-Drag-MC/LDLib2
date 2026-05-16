package com.lowdragmc.lowdraglib2.nodegraphtookit.editor;

import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves an {@link IResourcePath} to a loaded {@link Graph}, and optionally persists changes
 * back. Used by external (asset) subgraph nodes to fetch the inner graph on demand, and by the
 * editor when the user saves a dive-in view of an external subgraph (which must write back to
 * the original resource, not the host graph).
 *
 * <p>The editor wires a resolver into the graph model when loading; outside an editor context it
 * stays null and external subgraph nodes fall back to their cached port shape.</p>
 */
public interface IGraphReferenceResolver {
    /** Load a fresh {@link Graph} for the given path, or {@code null} if unresolvable. */
    @Nullable
    Graph resolve(IResourcePath path);

    /**
     * Persist {@code tag} as the resource at {@code path} and broadcast the change so other
     * editors holding references can refresh. Default is a no-op for read-only contexts.
     */
    default void save(IResourcePath path, CompoundTag tag) {
        // no-op default; editor resolvers override
    }

    /**
     * Returns the {@link GraphResource} that produced the graph this resolver is bound to, or
     * {@code null} if unknown. Used by drop-target validation to compare by resource identity
     * (resources are typically singletons), avoiding cross-resource imports without needing
     * to compare graph classes (a graph class can be shared by multiple resources).
     */
    @Nullable
    default GraphResource<?> getSourceResource() {
        return null;
    }
}
