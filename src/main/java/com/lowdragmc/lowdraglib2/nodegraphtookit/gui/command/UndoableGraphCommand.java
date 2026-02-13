package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import org.jetbrains.annotations.NotNull;

public abstract class UndoableGraphCommand implements IUndoableGraphCommand, EditAction {
    // runtime
    protected GraphView view;
    protected GraphModel graphModel;

    @Override
    public EditAction getEditAction(@NotNull GraphView view, @NotNull GraphModel graphModel) {
        this.view = view;
        this.graphModel = graphModel;
        generalActionData();
        return this;
    }

    protected void generalActionData() {

    }
}
