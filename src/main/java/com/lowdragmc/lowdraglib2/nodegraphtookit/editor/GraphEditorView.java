package com.lowdragmc.lowdraglib2.nodegraphtookit.editor;

import com.google.common.util.concurrent.Runnables;
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
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class GraphEditorView extends View {
    public final GraphView graphView = new GraphView();
    public final Button saveButton = new Button();

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
        graphView.header.select("#header-left").findFirst().ifPresent(e -> e.addChildAt(saveButton, 0));
        addChildren(graphView);
    }

    public GraphEditorView loadGraph(Graph graph, Consumer<CompoundTag> onSaved) {
        clear();
        this.graph = graph;
        this.onSaved = onSaved;
        graphView.loadGraph(graph);
        this.savedTag = serializeGraph();
        return this;
    }

    public CompoundTag serializeGraph() {
        if (graph == null) return new CompoundTag();
        return graph.graphModel.serializeNBT(Platform.getFrozenRegistry());
    }

    public GraphEditorView clear() {
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
        return graphView.getHistoryStack().getUndoStack().size() > 1;
    }

    private boolean canRedo() {
        return !graphView.getHistoryStack().getRedoStack().isEmpty();
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
            graphView.getHistoryStack().redo();
        }
        if (CommandEvents.UNDO.equals(event.command) && canUndo()) {
            graphView.getHistoryStack().undo();
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
