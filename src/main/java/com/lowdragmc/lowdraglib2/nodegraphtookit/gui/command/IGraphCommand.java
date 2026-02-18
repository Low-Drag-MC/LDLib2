package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import org.jetbrains.annotations.NotNull;

public interface IGraphCommand {
    /**
     * Executes the command.
     * @param view the graph view
     * @param graphModel the graph model
     */
    void execute(@NotNull GraphView view, @NotNull GraphModel graphModel);
}
