package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.gui.ui.utils.HistoryStack;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import org.jetbrains.annotations.NotNull;

public abstract class UndoableGraphCommand implements IUndoableGraphCommand {
    // runtime
    protected GraphView view;
    protected GraphModel graphModel;

    @Override
    public void execute(@NotNull GraphView view, @NotNull GraphModel graphModel, @NotNull HistoryStack historyStack) {
        this.view = view;
        this.graphModel = graphModel;
        generalActionData();

        var provider = Platform.getFrozenRegistry();
        var beforeTag = graphModel.serializeNBT(provider);

        execute();

        var afterTag = graphModel.serializeNBT(provider);

        historyStack.pushHistory(getCommandName(), EditAction.of(
                () -> { graphModel.deserializeNBT(provider, afterTag); view.rebuildGraphUI(); },
                () -> { graphModel.deserializeNBT(provider, beforeTag); view.rebuildGraphUI(); }
        ), getSource(), false);
    }

    /**
     * Executes the forward action of this command.
     * Undo is handled by the snapshot mechanism.
     */
    public abstract void execute();

    protected void generalActionData() {

    }
}
