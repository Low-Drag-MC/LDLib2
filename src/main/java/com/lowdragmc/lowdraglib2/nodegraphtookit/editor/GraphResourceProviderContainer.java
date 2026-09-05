package com.lowdragmc.lowdraglib2.nodegraphtookit.editor;

import com.google.common.collect.Maps;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

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
    private final Map<UUID, Pair<IResourcePath, GraphEditorView>> openedViews = Maps.newHashMap();

    public GraphResourceProviderContainer(GraphResource<G> graphResource, IResourceProvider<CompoundTag> provider) {
        super(provider);
        this.graphResource = graphResource;
        this.graphViewFactory = graphResource.getGraphViewFactory();
        // Dragging a graph resource carries the IResourcePath itself so the drop site (e.g. an open
        // GraphView) can record a stable EXTERNAL reference. Default behavior would drag the
        // CompoundTag NBT, which loses the path identity.
        this.onDragProvider = path -> new DraggingGraph(graphResource, path);

        setAddDefault(this::serializeDefaultGraph);

        // A graph editor has a viewing mode, so "not editable" is no reason not to open it: a built-in
        // blueprint is meant to be read — that is most of what makes it a worked example.
        setCanOpen(path -> true);

        setUiSupplier(path -> new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }).style(style -> style.backgroundTexture(graphResource.getIcon())));

        setOnEdit(this::openGraphForEdit);
    }

    // ---- template hooks — override these instead of re-implementing the open flow -----------------

    /** The stored form of a freshly-added resource — the resource's own tag form. */
    protected CompoundTag serializeDefaultGraph() {
        return graphResource.serializeGraphResource(graphResource.createGraph());
    }

    /** Build the editable graph instance from its stored tag — the resource's own load path
     *  (settings unwrapping, fixed-node restoration...). Override only for edit-specific needs. */
    protected G loadGraphForEdit(CompoundTag tag, IGraphReferenceResolver resolver) {
        return graphResource.deserializeGraphResource(tag, resolver);
    }

    /** The editor view hosting an opened graph. Override to supply a custom subclass (e.g. one
     *  whose {@code serializeGraph} emits a wrapped tag for dirty-detection/save parity). */
    protected GraphEditorView createEditorView() {
        return new GraphEditorView(graphViewFactory);
    }

    /** The full double-click-to-edit flow. Subclasses normally override the hooks above, not this. */
    protected void openGraphForEdit(ResourceProviderContainer<CompoundTag> container, IResourcePath path) {
        // if there is an existing view open, don't open a new one
        if (openedViews.values().stream().map(Pair::left).anyMatch(path::equals)) return;

        var tag = resourceProvider.getResource(path);
        if (tag == null) return;

        var resolver = createReferenceResolver(container);
        var graph = loadGraphForEdit(tag, resolver);
        if (graph == null) return;

        var editor = container.getEditor();
        var uuid = UUID.randomUUID();

        // Read-only-ness comes from the provider, not from this container: the same blueprint opened
        // out of the built-in library and out of a copy in the user's folder is the same graph, and
        // only the provider knows which of the two this path is.
        var editorView = createEditorView();
        editorView.setReadOnly(!resourceProvider.canEdit(path));
        var newView = editorView.loadGraph(graph, savedTag -> {
            if (!openedViews.containsKey(uuid)) return;
            var realPath = openedViews.get(uuid).left();
            resourceProvider.addResource(realPath, savedTag);
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
            openedViews.put(uuid, Pair.of(pathCache.get(), newView));
        });
        newView.addEventListener(UIEvents.REMOVED, e -> {
            var pair = openedViews.remove(uuid);
            if (pair != null) {
                pathCache.set(pair.left());
            }
        });
        newView.setCanRemove(true);
        newView.setIcon(graphResource.getIcon());
        newView.setDynamicName(() -> {
            if (openedViews.containsKey(uuid)) {
                return Component.literal(openedViews.get(uuid).left().getResourceName());
            } else {
                return Component.literal(pathCache.get().getResourceName());
            }
        });
        editor.placeView(newView, () -> editor.centerWindow.getLeftTop());
    }

    /**
     * The resolver installed on graphs opened from this container. Override to customize reference
     * resolution/saving WITHOUT re-implementing the whole open flow. Semantics of the default:
     * resolve tries the host provider first, then a cross-resource lookup ({@link #resolveForeign});
     * save writes into the SAME library resolve reads from (read/save symmetry) — a cross-library
     * reference saved through this resolver must never be deposited into the host library.
     */
    protected IGraphReferenceResolver createReferenceResolver(ResourceProviderContainer<CompoundTag> container) {
        return new IGraphReferenceResolver() {
            @Override
            public Graph resolve(IResourcePath refPath) {
                if (refPath == null) return null;
                // 1. Host resource fast-path (same-type subgraph) — the resource's own load path,
                //    threading this resolver so nested references keep resolving.
                var refTag = resourceProvider.getResource(refPath);
                if (refTag != null) {
                    return graphResource.deserializeGraphResource(refTag, this);
                }
                // 2. Cross-type: find the GraphResource that owns refPath among the editor's
                //    loaded resources and build the inner graph from it.
                return resolveForeign(container, refPath);
            }

            @Override
            public void save(IResourcePath refPath, CompoundTag refTag) {
                saveRouted(container, resourceProvider, refPath, refTag);
            }

            @Override
            public GraphResource<?> getSourceResource() {
                return graphResource;
            }
        };
    }

    /**
     * The canonical resolver-save: writes {@code refTag} into the library that owns {@code refPath}
     * — the host provider when it owns the path (the common same-library case), else the foreign
     * resource instance {@link #resolveForeign} would read from. Previously a cross-library save
     * silently deposited the tag into the HOST library (never updating the real one, and shadowing
     * the path with a wrong-typed resource). Broadcasts via {@link SubgraphRegistry} on success.
     */
    public static void saveRouted(ResourceProviderContainer<CompoundTag> container,
                                  IResourceProvider<CompoundTag> hostProvider,
                                  IResourcePath refPath, CompoundTag refTag) {
        if (refPath == null || refTag == null) return;
        if (hostProvider.hasResource(refPath)) {
            hostProvider.addResource(refPath, refTag);
            container.reloadSpecificResource(refPath);
            // Tell every open editor that this path was just saved. Listeners may need to refresh
            // their subgraph nodes' ports (if they reference path) or fully reload (root IS path).
            SubgraphRegistry.INSTANCE.notifyExternalGraphSaved(refPath);
            return;
        }
        if (saveForeign(container, refPath, refTag)) {
            SubgraphRegistry.INSTANCE.notifyExternalGraphSaved(refPath);
        } else {
            LDLib2.LOGGER.warn(
                    "Cannot save external graph {}: no editable provider owns it (read-only or unknown path).",
                    refPath);
        }
    }

    /** Write into the foreign resource instance that owns {@code path} — same iteration order as
     *  {@link #resolveForeign}, keeping read and save symmetric. Skips read-only providers. */
    @SuppressWarnings("unchecked")
    private static boolean saveForeign(ResourceProviderContainer<CompoundTag> container,
                                       IResourcePath path, CompoundTag tag) {
        var editor = container.getEditor();
        if (editor == null) return false;
        for (var entry : editor.resourceView.getResources().entrySet()) {
            if (!(entry.getKey() instanceof GraphResource<?>)) continue;
            if (!(entry.getValue().getResource(path) instanceof CompoundTag)) continue;
            var instance = (ResourceInstance<CompoundTag>) entry.getValue();
            for (var providers : instance.getBuiltinProviders().values()) {
                for (var candidate : providers) {
                    if (candidate.hasResource(path) && candidate.canEdit(path)) {
                        candidate.addResource(path, tag);
                        return true;
                    }
                }
            }
            for (var providers : instance.getCustomProviders().values()) {
                for (var candidate : providers) {
                    if (candidate.hasResource(path) && candidate.canEdit(path)) {
                        candidate.addResource(path, tag);
                        return true;
                    }
                }
            }
            return false; // the owner was found but is read-only — don't fall through to other libraries
        }
        return false;
    }

    /**
     * Resolves {@code path} against every {@link GraphResource} loaded in the editor — used when a
     * subgraph reference points at a graph of a different type than the host. Deserialization goes
     * through the OWNING resource's {@link GraphResource#deserializeGraphResource own load path}
     * (settings unwrapping, fixed-node restore), not a bare model read.
     */
    @org.jetbrains.annotations.Nullable
    protected static Graph resolveForeign(ResourceProviderContainer<CompoundTag> container, IResourcePath path) {
        var editor = container.getEditor();
        if (editor == null) return null;
        for (var entry : editor.resourceView.getResources().entrySet()) {
            if (!(entry.getKey() instanceof GraphResource<?> graphResource)) continue;
            var instance = entry.getValue();
            var refTag = instance.getResource(path);
            if (refTag instanceof CompoundTag tag) {
                return graphResource.deserializeGraphResource(tag, null);
            }
        }
        return null;
    }

    @Override
    protected void onRename(IResourcePath oldPath, IResourcePath newPath) {
        super.onRename(oldPath, newPath);
        for (var entry : openedViews.entrySet()) {
            var openedView = entry.getValue();

            if (openedView.left().equals(oldPath)) {
                entry.setValue(
                        Pair.of(newPath, openedView.right())
                );
            }
        }
    }
}
