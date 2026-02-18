package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.UUID;
import java.util.function.Consumer;

public record GraphNodeCreationData(GraphModel graphModel, Vector2f position, @Nullable SpawnFlags spawnFlags, @Nullable UUID uuid) {
    public static GraphNodeCreationData ofOrphan(GraphModel graphModel) {
        return new GraphNodeCreationData(graphModel, new Vector2f(), SpawnFlags.ORPHAN, null);
    }

    /**
     * Creates a new node in the graph.
     */
    public AbstractNodeModel createNode(Class<?> nodeType, String nodeName, Consumer<AbstractNodeModel> initializationCallback) {
        return graphModel().createNode(nodeType, nodeName, position(), uuid(), initializationCallback, spawnFlags());
    }

    /**
     * Creates a constant node in the graph.
     */
    public AbstractNodeModel createConstantNode(String constantName, TypeHandle typeHandle) {
        return graphModel().createConstantNode(typeHandle, constantName, position(), uuid(), null, spawnFlags());
    }
}
