package com.lowdragmc.lowdraglib2.nodegraphtookit.editor;

import com.google.common.collect.Maps;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourceProvider;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class GraphResourceProviderContainer<G extends Graph> extends ResourceProviderContainer<CompoundTag> {
    private final GraphResource<G> graphResource;
    private final Map<UUID, Tuple<IResourcePath, GraphEditorView>> openedViews = Maps.newHashMap();

    public GraphResourceProviderContainer(GraphResource<G> graphResource, IResourceProvider<CompoundTag> provider) {
        super(provider);
        this.graphResource = graphResource;

        setAddDefault(() -> {
            var graph = graphResource.createGraph();
            var output = TagValueOutput.createWithContext(ProblemReporter.Collector.DISCARDING, Platform.getFrozenRegistry());
            graph.graphModel.serialize(output);
            return output.buildResult();
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
            graph.graphModel.deserialize(TagValueInput.create(ProblemReporter.Collector.DISCARDING, Platform.getFrozenRegistry(), tag));

            var editor = container.getEditor();
            var uuid = UUID.randomUUID();

            var newView = new GraphEditorView().loadGraph(graph, savedTag -> {
                if (!openedViews.containsKey(uuid)) return;
                var realPath = openedViews.get(uuid).getA();
                provider.addResource(realPath, savedTag);
                container.reloadSpecificResource(realPath);
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
