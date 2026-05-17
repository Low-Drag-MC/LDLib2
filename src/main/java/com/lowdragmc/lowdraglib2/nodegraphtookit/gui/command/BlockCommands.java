package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomBlockNodeModelImpl;
import net.minecraft.network.chat.Component;

/**
 * Commands for mutating a {@link ContextNodeModel}'s block list. All three commands route through
 * {@link UndoableGraphCommand}, which snapshots the whole graph NBT before/after — so undo is
 * automatic, no inverse logic required here.
 */
public final class BlockCommands {

    public static class InsertBlockCommand extends UndoableGraphCommand {
        public static final Component NAME = Component.translatable("graph.commands.insert_block");
        private final ContextNodeModel contextNodeModel;
        private final Class<? extends BlockNode> blockType;
        private final int index;

        /**
         * @param contextNodeModel target context
         * @param blockType        block class to instantiate (must have a no-arg constructor)
         * @param index            insertion position; {@code -1} appends
         */
        public InsertBlockCommand(ContextNodeModel contextNodeModel,
                                  Class<? extends BlockNode> blockType,
                                  int index) {
            this.contextNodeModel = contextNodeModel;
            this.blockType = blockType;
            this.index = index;
        }

        @Override
        public void execute() {
            Node blockUserNode;
            try {
                blockUserNode = blockType.getConstructor().newInstance();
            } catch (Exception e) {
                LDLib2.LOGGER.error("Failed to instantiate block class {}", blockType.getName(), e);
                return;
            }
            var blockModel = new CustomBlockNodeModelImpl();
            blockModel.setGraphModel(graphModel);
            blockModel.setSpawnFlags(SpawnFlags.DEFAULT);
            blockModel.initCustomNode(blockUserNode);
            blockModel.onCreateNode();
            contextNodeModel.insertBlock(blockModel, index);
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }
    }

    public static class RemoveBlockCommand extends UndoableGraphCommand {
        public static final Component NAME = Component.translatable("graph.commands.remove_block");
        private final BlockNodeModel block;

        public RemoveBlockCommand(BlockNodeModel block) {
            this.block = block;
        }

        @Override
        public void execute() {
            var parent = block.getContextNodeModel();
            if (parent == null) return;
            parent.removeBlock(block);
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }
    }

    public static class MoveBlockCommand extends UndoableGraphCommand {
        public static final Component NAME = Component.translatable("graph.commands.move_block");
        private final ContextNodeModel contextNodeModel;
        private final int from;
        private final int to;

        public MoveBlockCommand(ContextNodeModel contextNodeModel, int from, int to) {
            this.contextNodeModel = contextNodeModel;
            this.from = from;
            this.to = to;
        }

        @Override
        public void execute() {
            contextNodeModel.moveBlock(from, to);
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }
    }

    private BlockCommands() {}
}
