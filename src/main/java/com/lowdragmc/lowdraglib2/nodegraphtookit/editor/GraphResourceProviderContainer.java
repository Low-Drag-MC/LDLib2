package com.lowdragmc.lowdraglib2.nodegraphtookit.editor;

import com.google.common.collect.Maps;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourceProvider;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class GraphResourceProviderContainer<G extends Graph> extends ResourceProviderContainer<CompoundTag> {
    public record DraggingGraph(GraphResource<?> graphResource, IResourcePath path) {}

    @Getter
    private final GraphResource<G> graphResource;
    /**
     * Factory for the {@link GraphView} used by editor views opened from this container. Initialized
     * from {@link GraphResource#getGraphViewFactory()}; can be overridden per-container for a custom
     * {@code GraphView} subclass.
     */
    @Getter @Setter
    private Supplier<? extends GraphView> graphViewFactory;
    private final Map<UUID, Tuple<IResourcePath, GraphEditorView>> openedViews = Maps.newHashMap();

    public GraphResourceProviderContainer(GraphResource<G> graphResource, IResourceProvider<CompoundTag> provider) {
        super(provider);
        this.graphResource = graphResource;
        this.graphViewFactory = graphResource.getGraphViewFactory();
        // Dragging a graph resource carries the IResourcePath itself so the drop site (e.g. an open
        // GraphView) can record a stable EXTERNAL reference. Default behavior would drag the
        // CompoundTag NBT, which loses the path identity.
        this.onDragProvider = path -> new DraggingGraph(graphResource, path);

        setAddDefault(() -> {
            var graph = graphResource.createGraph();
            return graph.graphModel.serializeNBT(Platform.getFrozenRegistry());
        });

        setUiSupplier(path -> new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }).style(style -> style.backgroundTexture(graphResource.getIcon())));

        setOnEdit((container, path) -> {
            // if there is an existing view open, don't open a new one
            if (openedViews.values().stream().map(Tuple::getA).anyMatch(path::equals)) return;

            var tag = provider.getResource(path);
            if (tag == null) return;

            // deserialize into a fresh graph
            var graph = graphResource.createGraph();
            // Resolver for external subgraph nodes: loads a fresh Graph snapshot for the referenced
            // resource path. Resolves same-type refs against the host provider first, then falls
            // back to a cross-resource lookup so a graph can reference a subgraph of a *different*
            // (host-accepted) graph type.
            IGraphReferenceResolver resolver = new IGraphReferenceResolver() {
                @Override
                public Graph resolve(IResourcePath refPath) {
                    if (refPath == null) return null;
                    // 1. Host resource fast-path (same-type subgraph).
                    var refTag = provider.getResource(refPath);
                    if (refTag != null) {
                        var refGraph = graphResource.createGraph();
                        refGraph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), refTag);
                        return refGraph;
                    }
                    // 2. Cross-type: find the GraphResource that owns refPath among the editor's
                    //    loaded resources and build the inner graph from it.
                    return resolveForeign(container, refPath);
                }

                @Override
                public void save(IResourcePath refPath, CompoundTag refTag) {
                    if (refPath == null || refTag == null) return;
                    provider.addResource(refPath, refTag);
                    container.reloadSpecificResource(refPath);
                    // Tell every open editor that this path was just saved. Listeners may need
                    // to refresh their subgraph nodes' ports (if they reference path) or fully
                    // reload (if their root IS path).
                    SubgraphRegistry.INSTANCE.notifyExternalGraphSaved(refPath);
                }

                @Override
                public GraphResource<?> getSourceResource() {
                    return graphResource;
                }
            };
            graph.graphModel.setReferenceResolver(resolver);
            graph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), tag);
            // re-apply after deserialize since deserialize may have rebuilt nested local subgraphs
            // (whose resolver was set only via addLocalSubgraph during deserialize, which already
            // propagates — but external subgraph nodes loaded as siblings need this too).
            graph.graphModel.setReferenceResolver(resolver);

            var editor = container.getEditor();
            var uuid = UUID.randomUUID();

            var newView = new GraphEditorView(graphViewFactory).loadGraph(graph, savedTag -> {
                if (!openedViews.containsKey(uuid)) return;
                var realPath = openedViews.get(uuid).getA();
                provider.addResource(realPath, savedTag);
                container.reloadSpecificResource(realPath);
                // broadcast: every other open editor that references this path must refresh ports
                SubgraphRegistry.INSTANCE.notifyExternalGraphSaved(realPath);
            });
            // Tell the editor what path it represents at the root level, so when another editor
            // saves an external subgraph that happens to be this same path, this view can reload
            // its root graph instead of just refreshing ports.
            newView.setRootPath(path);

            // cache path for renaming cases
            AtomicReference<IResourcePath> pathCache = new AtomicReference<>(path);
            newView.addEventListener(UIEvents.ADDED, e -> {
                openedViews.put(uuid, new Tuple<>(pathCache.get(), newView));
            });
            newView.addEventListener(UIEvents.REMOVED, e -> {
                var pair = openedViews.remove(uuid);
                if (pair != null) {
                    pathCache.set(pair.getA());
                }
            });
            newView.setCanRemove(true);
            newView.setIcon(graphResource.getIcon());
            newView.setDynamicName(() -> {
                if (openedViews.containsKey(uuid)) {
                    return Component.literal(openedViews.get(uuid).getA().getResourceName());
                } else {
                    return Component.literal(pathCache.get().getResourceName());
                }
            });
            editor.placeView(newView, () -> editor.centerWindow.getLeftTop());
        });
    }

    /**
     * Resolves {@code path} against every {@link GraphResource} loaded in the editor — used when a
     * subgraph reference points at a graph of a different type than the host. Returns the first
     * resource that owns the path, instantiated and deserialized, or {@code null} if none does.
     */
    @org.jetbrains.annotations.Nullable
    private static Graph resolveForeign(ResourceProviderContainer<CompoundTag> container, IResourcePath path) {
        var editor = container.getEditor();
        if (editor == null) return null;
        for (var entry : editor.resourceView.getResources().entrySet()) {
            if (!(entry.getKey() instanceof GraphResource<?> graphResource)) continue;
            var instance = entry.getValue();
            var refTag = instance.getResource(path);
            if (refTag instanceof CompoundTag tag) {
                var refGraph = graphResource.createGraph();
                refGraph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), tag);
                return refGraph;
            }
        }
        return null;
    }

    @Override
    protected void onRename(IResourcePath oldPath, IResourcePath newPath) {
        super.onRename(oldPath, newPath);
        for (var openedView : openedViews.values()) {
            if (openedView.getA().equals(oldPath)) {
                openedView.setA(newPath);
                openedView.getB().setRootPath(newPath);
            }
        }
    }
}
