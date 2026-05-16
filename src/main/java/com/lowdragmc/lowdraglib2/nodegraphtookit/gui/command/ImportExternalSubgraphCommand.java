package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.UUID;

/**
 * Inserts a {@link SubgraphNodeModel} referencing an external graph by {@link IResourcePath}.
 * The inner graph is resolved lazily on the first {@code defineNode} via
 * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel#getReferenceResolver()}.
 */
public class ImportExternalSubgraphCommand extends UndoableGraphCommand {
    private final static Component NAME = Component.translatable("graph.commands.import_external_subgraph");

    private final IResourcePath path;
    private final Vector2f position;
    @Nullable
    private final UUID nodeUid;

    public ImportExternalSubgraphCommand(IResourcePath path, Vector2f position, @Nullable UUID nodeUid) {
        this.path = path;
        this.position = position;
        this.nodeUid = nodeUid;
    }

    public ImportExternalSubgraphCommand(IResourcePath path, Vector2f position) {
        this(path, position, null);
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
        if (path == null) {
            LDLib2.LOGGER.warn("Cannot import subgraph: path is null.");
            return;
        }

        var nodeName = path.getResourceName();
        // Set only the name — getTitle() falls back to translatable(name), so rename drives the
        // displayed title without us pinning a hard-coded Component here.
        var node = graphModel.createNodeWithType(
                SubgraphNodeModel.class, nodeName, position, nodeUid,
                n -> n.setExternalSubgraph(path), SpawnFlags.DEFAULT);
        node.defineNode();
    }
}
