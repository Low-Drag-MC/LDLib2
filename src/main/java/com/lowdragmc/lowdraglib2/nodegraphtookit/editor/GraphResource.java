package com.lowdragmc.lowdraglib2.nodegraphtookit.editor;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.resource.IResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class GraphResource<G extends Graph> extends Resource<CompoundTag> {

    /**
     * Factory to create a new empty graph instance.
     */
    public abstract G createGraph();

    /**
     * The STORED tag form of a graph — the single authority every consumer (editor container,
     * cross-library resolvers, runtimes) should use. Default: the raw graph-model NBT. Resources
     * that wrap the model in extra data (settings, fixed-node metadata...) override BOTH this and
     * {@link #deserializeGraphResource} as a pair.
     */
    public CompoundTag serializeGraphResource(G graph) {
        return graph.graphModel.serializeNBT(Platform.getFrozenRegistry());
    }

    /**
     * Build a live graph from this resource's stored tag form (the inverse of
     * {@link #serializeGraphResource}). The resolver is applied before AND after deserialize —
     * nested local subgraphs propagate it during the load, sibling external references need the
     * re-apply. Overrides must honor {@code resolver} and perform any type-specific restoration
     * (settings, fixed nodes) so EVERY load path — editor open, foreign reference resolution,
     * runtime compile — gets an equally-valid graph.
     */
    public G deserializeGraphResource(CompoundTag tag, @Nullable IGraphReferenceResolver resolver) {
        var graph = createGraph();
        graph.graphModel.setReferenceResolver(resolver);
        graph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), tag);
        graph.graphModel.setReferenceResolver(resolver);
        return graph;
    }

    /**
     * Factory for the {@link GraphView} used by editors opened for this resource (and their
     * subgraph dives). Override to plug in a custom {@code GraphView} subclass; defaults to the
     * built-in {@link GraphView}.
     */
    public Supplier<? extends GraphView> getGraphViewFactory() {
        return GraphView::new;
    }

    @Nullable
    @Override
    public Tag serializeResource(CompoundTag value, HolderLookup.Provider provider) {
        return value;
    }

    @Nullable
    @Override
    public CompoundTag deserializeResource(Tag nbt, HolderLookup.Provider provider) {
        return nbt instanceof CompoundTag tag ? tag : new CompoundTag();
    }

    @Override
    public ResourceProviderContainer<CompoundTag> createResourceProviderContainer(IResourceProvider<CompoundTag> provider) {
        return new GraphResourceProviderContainer<>(this, provider);
    }
}
