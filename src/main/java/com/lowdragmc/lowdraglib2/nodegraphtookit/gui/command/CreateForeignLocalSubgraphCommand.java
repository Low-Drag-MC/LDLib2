package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.UUID;

/**
 * Creates a new empty local subgraph of a <em>different</em> graph type ({@code graphType}) inline
 * inside the current graph, plus a {@link SubgraphNodeModel} node bound to it — i.e. a node that is
 * itself a subgraph of another graph type. The inner type must be accepted by the host graph via
 * {@link Graph#acceptsSubgraphGraph}. Same-type embedding is handled by
 * {@link CreateLocalSubgraphCommand}; this is the cross-type counterpart. Undo is handled by the
 * snapshot mechanism in {@link UndoableGraphCommand}.
 */
public class CreateForeignLocalSubgraphCommand extends UndoableGraphCommand {
    private final static Component NAME = Component.translatable("graph.commands.create_foreign_local_subgraph");

    private final Class<? extends Graph> graphType;
    private final String nodeName;
    private final Vector2f position;
    @Nullable
    private final UUID nodeUid;

    public CreateForeignLocalSubgraphCommand(Class<? extends Graph> graphType, String nodeName,
                                             Vector2f position, @Nullable UUID nodeUid) {
        this.graphType = graphType;
        this.nodeName = nodeName;
        this.position = position;
        this.nodeUid = nodeUid;
    }

    public CreateForeignLocalSubgraphCommand(Class<? extends Graph> graphType, String nodeName, Vector2f position) {
        this(graphType, nodeName, position, null);
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
        var sub = graphModel.createLocalSubgraphInstance(graphType);
        if (sub == null) {
            LDLib2.LOGGER.warn("Graph type {} does not accept inline subgraph of type {}",
                    graphModel.getClass().getName(), graphType.getName());
            return;
        }
        graphModel.addLocalSubgraph(sub);

        // Set only the name — getTitle() falls back to translatable(name), so rename drives the
        // displayed title without us pinning a hard-coded Component here.
        var node = graphModel.createNodeWithType(
                SubgraphNodeModel.class, nodeName, position, nodeUid,
                n -> n.setLocalSubgraph(sub), SpawnFlags.DEFAULT);
        node.defineNode();
    }
}
