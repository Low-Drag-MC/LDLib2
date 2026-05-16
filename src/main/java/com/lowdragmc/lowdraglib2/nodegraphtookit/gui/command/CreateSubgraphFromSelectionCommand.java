package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin command wrapper around {@link com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel#extractSelectionToLocalSubgraph}.
 * The actual extraction lives on the model so it's testable without a UI.
 */
public class CreateSubgraphFromSelectionCommand extends UndoableGraphCommand {
    private final static Component NAME = Component.translatable("graph.commands.create_subgraph_from_selection");

    private final List<GraphElementModel> selection;

    public CreateSubgraphFromSelectionCommand(List<? extends GraphElementModel> selection) {
        this.selection = new ArrayList<>(selection);
    }

    @Override
    public Component getCommandName() {
        return NAME;
    }

    @Override
    public void execute() {
        graphModel.extractSelectionToLocalSubgraph(selection, Platform.getFrozenRegistry());
    }
}
