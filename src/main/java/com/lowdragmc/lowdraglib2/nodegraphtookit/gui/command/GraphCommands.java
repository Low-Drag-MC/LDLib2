package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

public final class GraphCommands {
    public static class DeleteElementsCommand extends UndoableGraphCommand {
        public static final Component NAME = Component.translatable("graph.commands.delete");
        public List<GraphElementModel> elementsToDelete;

        public DeleteElementsCommand(List<GraphElementModel> elementsToDelete) {
            this.elementsToDelete = elementsToDelete;
        }

        @Override
        public void execute() {
            if (elementsToDelete.isEmpty()) return;
            graphModel.deleteElements(elementsToDelete);
            // todo
            elementsToDelete = Collections.emptyList();
        }

        @Override
        public void undo() {
            // todo
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }
    }
}
