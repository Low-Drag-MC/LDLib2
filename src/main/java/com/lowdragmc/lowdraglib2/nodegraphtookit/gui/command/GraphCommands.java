package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IMovable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Model;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

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
        public Component getCommandName() {
            return NAME;
        }
    }

    public static class MoveElementsCommand extends UndoableGraphCommand {
        public static final Component NAME = Component.translatable("graph.commands.move");
        private final List<Model> movables;
        private final Vector2f localOffset;

        public MoveElementsCommand(List<Model> movables, Vector2f localOffset) {
            this.movables = movables;
            this.localOffset = localOffset;
        }

        @Override
        public void execute() {
            for (var model : movables) {
                if (model instanceof IMovable movable) {
                    movable.setPosition(localOffset.add(movable.getPosition(), new Vector2f()));
                }
            }
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }
    }
}
