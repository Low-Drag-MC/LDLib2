package com.lowdragmc.lowdraglib2.nodegraphtookit.editor;

import com.google.common.util.concurrent.Runnables;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.IHistoryStack;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphBreadcrumb;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

public class GraphEditorView extends View {
    /** Root graph view — owns the editor's root graph and is always at the bottom of the navigation stack. */
    public final GraphView graphView = new GraphView();
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
    /** Subgraph navigation stack. Element 0 is always the root (graphView); top is current. */
    private final Deque<GraphView> viewStack = new ArrayDeque<>();
    /** Labels parallel to {@link #viewStack}, used to render the breadcrumb. */
    private final List<Component> pathLabels = new ArrayList<>();

    public GraphEditorView() {
        super("editor.view.graph_editor");
        addClass("__graph-editor-view__");

        saveButton.setOnClick(e -> notifySaved());
        saveButton.setText("ldlib.gui.editor.menu.save");
        saveButton.textStyle(style -> style.textColor(ColorPattern.GRAY.color));
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
        viewStack.push(graphView);
        attachOverlayToHeader(graphView);
        addChildren(graphView);
    }

    /**
     * Reattaches the saveButton + breadcrumb to a given GraphView's header. The UI framework
     * auto-removes them from any previous parent when re-parented.
     */
    private void attachOverlayToHeader(GraphView view) {
        view.header.select("#header-left").findFirst().ifPresent(e -> e.addChildAt(saveButton, 0));
        view.header.select("#header-center").findFirst().ifPresent(e -> e.addChild(breadcrumb));
    }

    /** Current (topmost) view in the subgraph navigation stack — always non-null after construction. */
    public GraphView getCurrentView() {
        return viewStack.peek();
    }

    /**
     * Pushes a new GraphView showing the inner graph of {@code subNode}. The current view stays in
     * memory (its history and viewport are preserved) but is detached from the DOM.
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

        var current = getCurrentView();
        // detach old
        removeChild(current);
        // build new
        var newView = new GraphView();
        newView.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        viewStack.push(newView);
        var nodeTitle = subNode.getTitle();
        pathLabels.add(nodeTitle == null ? Component.literal("Subgraph") : nodeTitle);
        attachOverlayToHeader(newView);
        addChildren(newView);
        newView.loadGraph(innerGraph);
        refreshBreadcrumb();
    }

    /**
     * Pops the navigation stack down to {@code level} (0 = root). No-op if already at that level.
     * The popped GraphView instances are discarded (their HistoryStack with them).
     */
    public void popToLevel(int level) {
        if (level < 0) level = 0;
        if (level >= viewStack.size()) return;
        var depth = viewStack.size() - 1; // index of top
        if (level == depth) return;
        // detach current
        var current = getCurrentView();
        removeChild(current);
        while (viewStack.size() - 1 > level) {
            viewStack.pop();
            if (!pathLabels.isEmpty()) pathLabels.remove(pathLabels.size() - 1);
        }
        var target = getCurrentView();
        attachOverlayToHeader(target);
        addChildren(target);
        refreshBreadcrumb();
    }

    private void refreshBreadcrumb() {
        var labels = new ArrayList<Component>();
        labels.add(Component.literal("root"));
        labels.addAll(pathLabels);
        breadcrumb.setPath(labels);
    }

    public GraphEditorView loadGraph(Graph graph, Consumer<CompoundTag> onSaved) {
        clear();
        this.graph = graph;
        this.onSaved = onSaved;
        // reset stack to root so a fresh open never inherits prior subgraph navigation
        popToLevel(0);
        graphView.loadGraph(graph);
        // register the root with the broadcast registry so external-save events can find us
        SubgraphRegistry.INSTANCE.register(graph.graphModel);
        refreshBreadcrumb();
        this.savedTag = serializeGraph();
        return this;
    }

    public CompoundTag serializeGraph() {
        if (graph == null) return new CompoundTag();
        return graph.graphModel.serializeNBT(Platform.getFrozenRegistry());
    }

    public GraphEditorView clear() {
        if (this.graph != null) {
            SubgraphRegistry.INSTANCE.unregister(this.graph.graphModel);
        }
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
        saveButton.textStyle(style -> style.textColor(ColorPattern.WHITE.color));
    }

    public void clearDirty() {
        isDirty = false;
        saveButton.setActive(false);
        saveButton.textStyle(style -> style.textColor(ColorPattern.GRAY.color));
    }

    public void notifySaved() {
        if (graph != null && onSaved != null) {
            onSaved.accept(serializeGraph());
        }
        this.savedTag = serializeGraph();
        clearDirty();
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
        // auto-detect dirtiness: current serialized graph differs from saved snapshot.
        // brute-force comparison; can be optimized later if it shows up in profiling.
        if (!isDirty && graph != null) {
            var mui = getModularUI();
            if (mui != null && (mui.getTickCounter() & 20) == 0) {
                if (!serializeGraph().equals(savedTag)) {
                    markAsDirty();
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
}
