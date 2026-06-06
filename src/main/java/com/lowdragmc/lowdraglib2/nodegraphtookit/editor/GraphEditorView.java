package com.lowdragmc.lowdraglib2.nodegraphtookit.editor;

import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.IHistoryStack;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphBreadcrumb;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GraphEditorView extends View implements SubgraphRegistry.Listener {
    /**
     * Factory used to create every {@link GraphView} this editor owns — the root view and any
     * subgraph-dive views. Defaults to {@link GraphView#GraphView()}; supply a custom factory
     * (via the {@link #GraphEditorView(Supplier) constructor}) to plug in a {@code GraphView}
     * subclass.
     */
    @Getter
    private final Supplier<? extends GraphView> graphViewFactory;
    /** Root graph view — owns the editor's root graph and is always at the bottom of the navigation stack. */
    public final GraphView graphView;
    public final Button saveButton = new Button();
    private final GraphBreadcrumb breadcrumb = new GraphBreadcrumb();

    // runtime
    private boolean isDirty;
    @Nullable @Getter
    private Graph graph;
    @Nullable @Getter
    private Consumer<CompoundTag> onSaved;
    /** The graph NBT captured at load/save time — used to detect dirtiness by comparison. */
    @Nullable
    private CompoundTag savedTag;
    // todo move to history stack
    private IHistoryStack.HistoryItem savedHistoryPoint;
    /**
     * Resource path that this editor view represents at the <em>root</em> level. Set by the
     * resource provider container when opening the view; used to recognize "another editor just
     * saved my path" broadcasts and reload accordingly.
     */
    @Nullable @Getter
    private IResourcePath rootPath;
    /** Subgraph navigation stack. Bottom entry is always the root level. */
    private final Deque<Level> levelStack = new ArrayDeque<>();
    /**
     * Set while we're driving our own save → broadcast. Lets us ignore our own
     * {@link #onExternalGraphSaved} callback so we don't pointlessly self-reload (and lose
     * UI state like selection and viewport).
     */
    private boolean isSavingSelf = false;

    /**
     * One entry in the navigation stack. The root entry has {@code externalPath == null} and
     * {@code graphRef == null} — the root graph is held on the enclosing editor's {@link #graph}.
     * An external dive entry holds its own {@link Graph} instance (loaded via the resolver) and
     * the path it was loaded from.
     */
    private static class Level {
        final GraphView view;
        final Component label;
        /** External path being edited at this level, or {@code null} for root / local-dive levels. */
        @Nullable final IResourcePath externalPath;
        /** Live Graph instance held by an external-dive level; {@code null} for root / local-dive. */
        @Nullable final Graph graphRef;
        @Nullable CompoundTag levelSavedTag;

        Level(GraphView view, Component label, @Nullable IResourcePath externalPath, @Nullable Graph graphRef) {
            this.view = view;
            this.label = label;
            this.externalPath = externalPath;
            this.graphRef = graphRef;
        }
    }

    public GraphEditorView() {
        this(GraphView::new);
    }

    /**
     * @param graphViewFactory factory used to create the root view and all subgraph-dive views,
     *                         allowing a custom {@link GraphView} subclass to be plugged in.
     */
    public GraphEditorView(Supplier<? extends GraphView> graphViewFactory) {
        super("editor.view.graph_editor");
        this.graphViewFactory = graphViewFactory;
        this.graphView = graphViewFactory.get();
        addClass("__graph-editor-view__");

        saveButton.setOnClick(e -> notifySaved());
        saveButton.setText("ldlib.gui.editor.menu.save");
        saveButton.setActive(false);

        // graphView fills remaining space
        graphView.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });

        setFocusable(true);
        addEventListener(UIEvents.VALIDATE_COMMAND, this::onValidateCommand);
        addEventListener(UIEvents.EXECUTE_COMMAND, this::onExecuteCommand);
        dynamicName = () -> Component.translatable(getName());
        breadcrumb.setOnJump(this::popToLevel);
        levelStack.push(new Level(graphView, Component.translatable("graph.breadcrumb.root"), null, null));
        attachOverlayToHeader(graphView);
        addChildren(graphView);
    }

    /** Called by the resource container to inform the view of the path it represents. */
    public void setRootPath(@Nullable IResourcePath path) {
        this.rootPath = path;
    }

    /**
     * Reattaches the saveButton + breadcrumb to a given GraphView's header. The UI framework
     * auto-removes them from any previous parent when re-parented.
     */
    private void attachOverlayToHeader(GraphView view) {
        view.header.select(".__node-graph-view_header-left__").findFirst().ifPresent(e -> e.addChildAt(saveButton, 0));
        view.header.select(".__node-graph-view_header-center__").findFirst().ifPresent(e -> e.addChild(breadcrumb));
    }

    /** Current (topmost) view in the subgraph navigation stack — always non-null after construction. */
    public GraphView getCurrentView() {
        return levelStack.peek().view;
    }

    private Level getCurrentLevel() {
        return levelStack.peek();
    }

    /**
     * Pushes a new GraphView showing the inner graph of {@code subNode}. The previous level stays
     * in memory (history + viewport preserved) but is detached from the DOM.
     */
    public void enterSubgraph(SubgraphNodeModel subNode) {
        if (subNode == null) return;
        var innerModel = subNode.getSubgraphModel();
        if (!(innerModel instanceof CustomGraphModelImpl custom)) {
            LDLib2.LOGGER.warn("Cannot enter subgraph — inner graph is not resolvable.");
            return;
        }
        var innerGraph = custom.getGraph();
        if (innerGraph == null) return;

        // Detach old top
        removeChild(getCurrentView());

        // Build new view (via the same factory as the root, so subgraph dives use the custom type too)
        var newView = graphViewFactory.get();
        newView.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        var nodeTitle = subNode.getTitle();
        var label = nodeTitle == null ? Component.translatable("graph.breadcrumb.subgraph") : nodeTitle;
        // EXTERNAL dive: track path + graph instance so save can write back through resolver
        var externalPath = subNode.getKind() == SubgraphNodeModel.Kind.EXTERNAL
                ? subNode.getExternalPath() : null;
        var graphRef = externalPath != null ? innerGraph : null;

        var level = new Level(newView, label, externalPath, graphRef);
        levelStack.push(level);
        attachOverlayToHeader(newView);
        addChildren(newView);
        newView.loadGraph(innerGraph);
        if (externalPath != null) {
            level.levelSavedTag = serializeLevelGraph(level);
        }
        refreshBreadcrumb();
        refreshSaveButton();
    }

    /**
     * Pops the navigation stack down to {@code level} (0 = root). No-op if already at that level.
     * Popped levels are discarded (their HistoryStack with them).
     */
    public void popToLevel(int level) {
        if (level < 0) level = 0;
        if (level >= levelStack.size()) return;
        var depth = levelStack.size() - 1; // index of top
        if (level == depth) return;
        removeChild(getCurrentView());
        while (levelStack.size() - 1 > level) {
            levelStack.pop();
        }
        var target = getCurrentLevel();
        attachOverlayToHeader(target.view);
        addChildren(target.view);
        refreshBreadcrumb();
        refreshSaveButton();
    }

    private void refreshBreadcrumb() {
        var labels = new ArrayList<Component>();
        // Stack iterates top→bottom; collect bottom→top
        var snapshot = new ArrayList<>(levelStack);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            labels.add(snapshot.get(i).label);
        }
        breadcrumb.setPath(labels);
    }

    public GraphEditorView loadGraph(Graph graph, Consumer<CompoundTag> onSaved) {
        clear();
        this.graph = graph;
        this.onSaved = onSaved;
        // reset stack to root so a fresh open never inherits prior subgraph navigation
        popToLevel(0);
        graphView.loadGraph(graph);
        // Subscribe to external-save broadcasts. Both the GraphModel form (port refresh) and the
        // Listener form (full reload when our root path matches) — the editor needs both.
        SubgraphRegistry.INSTANCE.register(graph.graphModel);
        SubgraphRegistry.INSTANCE.registerListener(this);
        refreshBreadcrumb();
        this.savedTag = serializeGraph();
        refreshSaveButton();
        return this;
    }

    public CompoundTag serializeGraph() {
        if (graph == null) return new CompoundTag();
        return graph.graphModel.serializeNBT(Platform.getFrozenRegistry());
    }

    private CompoundTag serializeLevelGraph(Level level) {
        if (level.graphRef == null) return new CompoundTag();
        return level.graphRef.graphModel.serializeNBT(Platform.getFrozenRegistry());
    }

    public GraphEditorView clear() {
        if (this.graph != null) {
            SubgraphRegistry.INSTANCE.unregister(this.graph.graphModel);
        }
        SubgraphRegistry.INSTANCE.unregisterListener(this);
        popToLevel(0);
        graphView.loadGraph(null);
        this.graph = null;
        this.onSaved = null;
        clearDirty();
        return this;
    }

    public void markAsDirty() {
        isDirty = true;
        saveButton.setActive(true);
    }

    public void clearDirty() {
        isDirty = false;
        saveButton.setActive(false);
    }

    /**
     * Saves the <em>current</em> level. For root / local-dive levels, that's the root graph via
     * the {@link #onSaved} callback. For an external-dive level, the level's own graph is written
     * back through the {@link IGraphReferenceResolver#save resolver}.
     */
    public void notifySaved() {
        isSavingSelf = true;
        try {
            var level = getCurrentLevel();
            if (level.externalPath != null && level.graphRef != null) {
                var resolver = graph != null ? graph.graphModel.getReferenceResolver() : null;
                if (resolver == null) {
                    LDLib2.LOGGER.warn("Cannot save external subgraph at {}: no resolver bound.", level.externalPath);
                    return;
                }
                var tag = serializeLevelGraph(level);
                resolver.save(level.externalPath, tag);
                level.levelSavedTag = tag;
                clearDirty();
                return;
            }
            // root / local-dive save path
            if (graph != null && onSaved != null) {
                onSaved.accept(serializeGraph());
            }
            this.savedTag = serializeGraph();
            clearDirty();
        } finally {
            isSavingSelf = false;
        }
    }

    /**
     * Refreshes the save button's active/inactive state for the current level's dirty status.
     * A level switch doesn't change dirtiness, just resets which graph we're comparing against.
     */
    private void refreshSaveButton() {
        // simplest: keep current isDirty for root; for external dive, recompute against levelSavedTag
        // (the screenTick auto-detect path keeps this fresh).
        if (isDirty) markAsDirty();
        else clearDirty();
    }

    private boolean canUndo() {
        // Need at least 2 entries: the initial state at the bottom + at least one change
        return getCurrentView().getHistoryStack().getUndoStack().size() > 1;
    }

    private boolean canRedo() {
        return !getCurrentView().getHistoryStack().getRedoStack().isEmpty();
    }

    protected void onValidateCommand(UIEvent event) {
        if (CommandEvents.REDO.equals(event.command) && canRedo()) {
            event.stopPropagation();
        }
        if (CommandEvents.UNDO.equals(event.command) && canUndo()) {
            event.stopPropagation();
        }
    }

    protected void onExecuteCommand(UIEvent event) {
        if (CommandEvents.REDO.equals(event.command) && canRedo()) {
            getCurrentView().getHistoryStack().redo();
        }
        if (CommandEvents.UNDO.equals(event.command) && canUndo()) {
            getCurrentView().getHistoryStack().undo();
        }
    }

    @Override
    protected void onAdded() {
        super.onAdded();
        if (this.graph != null) {
            graphView.loadGraph(this.graph);
        }
    }

    @Override
    public void screenTick() {
        super.screenTick();
        // Auto-detect dirtiness: compare the current level's graph serialization against its
        // last-saved snapshot. Brute-force comparison; can be optimized later if it shows up.
        if (!isDirty) {
            var mui = getModularUI();
            if (mui != null && (mui.getTickCounter() & 20) == 0) {
                var level = getCurrentLevel();
                if (level.externalPath != null && level.graphRef != null) {
                    var tag = serializeLevelGraph(level);
                    if (level.levelSavedTag == null || !tag.equals(level.levelSavedTag)) {
                        markAsDirty();
                    }
                } else if (graph != null) {
                    if (!serializeGraph().equals(savedTag)) {
                        markAsDirty();
                    }
                }
            }
        }
    }

    @Override
    protected Component getViewName() {
        var viewName = super.getViewName();
        if (isDirty) {
            return viewName.copy().append(" *");
        }
        return viewName;
    }

    @Override
    protected void onClose() {
        if (isDirty) {
            Dialog.showCancelableCheck("Dialog.notify", "view.save_before_close.info", save -> {
                if (isCanRemove()) {
                    if (save) {
                        notifySaved();
                    }
                    removeSelf();
                }
            }, Runnables.doNothing()).show(getModularUI());
        } else {
            removeSelf();
        }
    }

    // ---------------- SubgraphRegistry.Listener ----------------

    /**
     * Another save event just landed for {@code path}. If our root represents the same path,
     * reload it (unless we have unsaved changes — never silently throw those away). Otherwise
     * port refresh is already handled via the GraphModel-mode registration.
     */
    @Override
    public void onExternalGraphSaved(IResourcePath path) {
        if (isSavingSelf) return; // ignore our own broadcast
        if (path == null || rootPath == null || graph == null) return;
        if (!path.equals(rootPath)) return;
        if (isDirty) {
            LDLib2.LOGGER.warn(
                    "External save for {} arrived but this editor has unsaved changes — skipping reload.",
                    path);
            return;
        }
        var resolver = graph.graphModel.getReferenceResolver();
        if (resolver == null) return;
        var fresh = resolver.resolve(path);
        if (fresh == null) return;
        // Preserve the save callback across reload. loadGraph re-registers listener/root,
        // which is idempotent for the Listener set.
        var savedCb = this.onSaved;
        loadGraph(fresh, savedCb);
    }
}
