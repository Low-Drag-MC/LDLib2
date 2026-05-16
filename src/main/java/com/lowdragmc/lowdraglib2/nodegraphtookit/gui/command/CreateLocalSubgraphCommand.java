package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.UUID;

/**
 * Creates a new empty local subgraph attached to the current graph, plus a
 * {@link SubgraphNodeModel} node bound to it. Undo handled by the snapshot mechanism in
 * {@link UndoableGraphCommand}.
 */
public class CreateLocalSubgraphCommand extends UndoableGraphCommand {
    private final static Component NAME = Component.translatable("graph.commands.create_local_subgraph");

    private final Vector2f position;
    @Nullable
    private final UUID nodeUid;

    public CreateLocalSubgraphCommand(Vector2f position, @Nullable UUID nodeUid) {
        this.position = position;
        this.nodeUid = nodeUid;
    }

    public CreateLocalSubgraphCommand(Vector2f position) {
        this(position, null);
    }

    @Override
    public Component getCommandName() {
        return NAME;
    }

    @Override
    public void execute() {
        if (!graphModel.allowSubgraphCreation()) {
            LDLib2.LOGGER.warn("Subgraph creation is disabled on this graph.");
            return;
        }
        var sub = graphModel.createLocalSubgraphInstance();
        if (sub == null) {
            LDLib2.LOGGER.warn("Graph type does not support inline subgraphs: {}", graphModel.getClass().getName());
            return;
        }
        graphModel.addLocalSubgraph(sub);

        // Set only the name — getTitle() falls back to translatable(name), so rename drives the
        // displayed title without us pinning a hard-coded Component here.
        var node = graphModel.createNodeWithType(
                SubgraphNodeModel.class, "Subgraph", position, nodeUid,
                n -> n.setLocalSubgraph(sub), SpawnFlags.DEFAULT);
        node.defineNode();
    }
}
