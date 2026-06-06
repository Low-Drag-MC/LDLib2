package com.lowdragmc.lowdraglib2.nodegraphtookit.editor;

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
