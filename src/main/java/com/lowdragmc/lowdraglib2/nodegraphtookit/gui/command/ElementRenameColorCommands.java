package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IHasElementColor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IHasName;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Undoable commands for {@link IHasName#setName} and {@link IHasElementColor#setColor} on any
 * graph element. Both go through the snapshot mechanism in {@link UndoableGraphCommand}, so undo
 * is essentially "deserialize the pre-change tag back" — no custom inverse logic needed.
 *
 * <p>We persist the target by UUID, not by reference, so undo/redo after a graph round-trip
 * still resolves the right element.</p>
 */
public final class ElementRenameColorCommands {
    private ElementRenameColorCommands() {}

    public static class RenameElementCommand extends UndoableGraphCommand {
        private static final Component NAME = Component.translatable("graph.commands.rename");
        private final UUID targetUid;
        private final String newName;

        public RenameElementCommand(GraphElementModel target, String newName) {
            this.targetUid = target == null ? null : target.getUid();
            this.newName = newName;
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }

        @Override
        public void execute() {
            if (targetUid == null || newName == null) return;
            var model = graphModel.getModel(targetUid);
            if (model instanceof IHasName named) {
                named.setName(newName);
            }
        }
    }

    public static class SetElementColorCommand extends UndoableGraphCommand {
        private static final Component NAME = Component.translatable("graph.commands.color");
        private final UUID targetUid;
        private final int newColor;

        public SetElementColorCommand(GraphElementModel target, int newColor) {
            this.targetUid = target == null ? null : target.getUid();
            this.newColor = newColor;
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }

        @Override
        public void execute() {
            if (targetUid == null) return;
            var model = graphModel.getModel(targetUid);
            if (model instanceof IHasElementColor colored) {
                colored.setColor(newColor);
            }
        }
    }

    /**
     * Reverts {@link IHasElementColor#hasUserColor()} → false and the visible color back to
     * {@link IHasElementColor#getDefaultColor()}. Snapshot/undo restores the previous state.
     */
    public static class ResetElementColorCommand extends UndoableGraphCommand {
        private static final Component NAME = Component.translatable("graph.commands.color_reset");
        private final UUID targetUid;

        public ResetElementColorCommand(GraphElementModel target) {
            this.targetUid = target == null ? null : target.getUid();
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }

        @Override
        public void execute() {
            if (targetUid == null) return;
            var model = graphModel.getModel(targetUid);
            if (model instanceof IHasElementColor colored) {
                colored.resetColor();
            }
        }
    }
}
