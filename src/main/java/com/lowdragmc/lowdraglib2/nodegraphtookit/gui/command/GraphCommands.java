package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IMovable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Model;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.PlacematModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.StickyNoteModel;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

import java.util.ArrayList;
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
                    var newPos = localOffset.add(movable.getPosition(), new Vector2f());
                    if (view != null) newPos = view.snapPosition(newPos);
                    movable.setPosition(newPos);
                }
            }
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }
    }

    public static class PasteElementsCommand extends UndoableGraphCommand {
        public static final Component NAME = Component.translatable("ldlib.gui.editor.menu.paste");
        private final GraphModel.CopyPasteData data;
        private final Vector2f positionOffset;

        public PasteElementsCommand(GraphModel.CopyPasteData data, Vector2f positionOffset) {
            this.data = data;
            this.positionOffset = positionOffset;
        }

        @Override
        public void execute() {
            var pasted = graphModel.pasteElements(data, positionOffset);
            // Select the pasted elements
            view.clearAllSelected();
            view.rebuildGraphUI();
            for (var element : pasted) {
                view.addSelected(element);
            }
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }
    }

    public static class CreatePlacematCommand extends UndoableGraphCommand {
        public static final Component NAME = Component.translatable("graph.commands.create_placemat");
        private final String name;
        private final Vector2f position;
        private final Vector2f size;

        public CreatePlacematCommand(String name, Vector2f position, Vector2f size) {
            this.name = name;
            this.position = position;
            this.size = size;
        }

        @Override
        public void execute() {
            var placemat = graphModel.createPlacemat(name, position, size);
            view.clearAllSelected();
            view.addSelected(placemat);
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }
    }

    public static class CreateStickyNoteCommand extends UndoableGraphCommand {
        public static final Component NAME = Component.translatable("graph.commands.create_sticky_note");
        private final Vector2f position;

        public CreateStickyNoteCommand(Vector2f position) {
            this.position = position;
        }

        @Override
        public void execute() {
            var stickyNote = graphModel.createStickyNote(position);
            view.clearAllSelected();
            view.addSelected(stickyNote);
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }
    }
}
