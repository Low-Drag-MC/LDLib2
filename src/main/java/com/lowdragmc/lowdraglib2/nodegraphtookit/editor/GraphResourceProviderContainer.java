package com.lowdragmc.lowdraglib2.nodegraphtookit.editor;

import com.google.common.collect.Maps;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourceProvider;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class GraphResourceProviderContainer<G extends Graph> extends ResourceProviderContainer<CompoundTag> {
    public record DraggingGraph(GraphResource<?> graphResource, IResourcePath path) {}

    @Getter
    private final GraphResource<G> graphResource;
    private final Map<UUID, Tuple<IResourcePath, GraphEditorView>> openedViews = Maps.newHashMap();

    public GraphResourceProviderContainer(GraphResource<G> graphResource, IResourceProvider<CompoundTag> provider) {
        super(provider);
        this.graphResource = graphResource;
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
            // resource path. Same GraphResource type assumed; cross-type subgraphs are out of scope
            // for v1.
            IGraphReferenceResolver resolver = refPath -> {
                if (refPath == null) return null;
                var refTag = provider.getResource(refPath);
                if (refTag == null) return null;
                var refGraph = graphResource.createGraph();
                refGraph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), refTag);
                return refGraph;
            };
            graph.graphModel.setReferenceResolver(resolver);
            graph.graphModel.deserializeNBT(Platform.getFrozenRegistry(), tag);
            // re-apply after deserialize since deserialize may have rebuilt nested local subgraphs
            // (whose resolver was set only via addLocalSubgraph during deserialize, which already
            // propagates — but external subgraph nodes loaded as siblings need this too).
            graph.graphModel.setReferenceResolver(resolver);

            var editor = container.getEditor();
            var uuid = UUID.randomUUID();

            var newView = new GraphEditorView().loadGraph(graph, savedTag -> {
                if (!openedViews.containsKey(uuid)) return;
                var realPath = openedViews.get(uuid).getA();
                provider.addResource(realPath, savedTag);
                container.reloadSpecificResource(realPath);
                // broadcast: every other open editor that references this path must refresh ports
                SubgraphRegistry.INSTANCE.notifyExternalGraphSaved(realPath);
            });

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
            editor.centerWindow.getLeftTop().addView(newView);
        });
    }

    @Override
    protected void onRename(IResourcePath oldPath, IResourcePath newPath) {
        super.onRename(oldPath, newPath);
        for (var openedView : openedViews.values()) {
            if (openedView.getA().equals(oldPath)) {
                openedView.setA(newPath);
            }
        }
    }
}
