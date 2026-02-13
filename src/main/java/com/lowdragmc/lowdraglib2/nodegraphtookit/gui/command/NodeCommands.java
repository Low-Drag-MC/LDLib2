package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.GraphNodeCreationData;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.NodeItemLibraryData;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.NodeModelLibraryItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.InputOutputPortsNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.IGhostWireModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireSide;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class NodeCommands {
    public static class CreateNodeCommand extends UndoableGraphCommand {
        private final static Component NAME = Component.translatable("graph.commands.create_node");

        /**
         * Command to create a node from a {@link NodeModelLibraryItem}
         * @param position position to create the node at
         * @param wireToInsertOn The wire model on which to insert the newly created node.
         * @param portModel The port to which to connect the new node.
         * @param nodeLibraryItem representing the node to create.
         * @param uid The guid to assign to the newly created item.
         * @param wiresToConnect The wire models on which to connect the newly created node.
         */
        public record NodeData(
                Vector2f position,
                WireModel wireToInsertOn,
                PortModel portModel,
//                VariableDeclarationModelBase variableDeclaration,
                NodeModelLibraryItem nodeLibraryItem,
                UUID uid,
                List<Pair<WireModel, WireSide>> wiresToConnect
//                VariableCreationInfos variableCreationInfos,
//                UUID variableDeclarationUid;
        ) {}

        public enum ConnectionsToMake {
            NONE,
            EXISTING_PORT,
            INSERT_ON_WIRE,
            EXISTING_WIRES
        };

        protected final List<NodeData> creationData = new ArrayList<>();

        public CreateNodeCommand() {}


        public CreateNodeCommand withNode(NodeData data) {
            creationData.add(data);
            return this;
        }

        /**
         * Add a node from an item inserted on the graph.
         */
        public CreateNodeCommand onGraph(NodeModelLibraryItem item,
                                         Vector2f position,
                                         @Nullable UUID uid) {
            return withNode(new NodeData(position, null, null, item, uid, null));
        }

        /**
         * Add a node from an item inserted on wires.
         */
        public CreateNodeCommand withNodeOnWires(NodeModelLibraryItem item,
                                    List<Pair<WireModel, WireSide>> wiresToConnect,
                                    Vector2f position,
                                    @Nullable UUID uid) {
            return withNode(new NodeData(position, null, null, item, uid, wiresToConnect));
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }

        @Override
        public void execute() {
            if (creationData.isEmpty()) return;
            // validation
//            if (command.CreationData.All(nodeData => nodeData.VariableDeclaration == null) && command.CreationData.All(nodeData => nodeData.NodeLibraryItem == null) && command.CreationData.All(nodeData => nodeData.VariableCreationInfos == null))
//                return;

            var elementsToSelect = new ArrayList<GraphElementModel>();

            for (var nodeData : creationData) {

                var connectionsToMake = ConnectionsToMake.NONE;

                if (nodeData.portModel != null) {
                    connectionsToMake = ConnectionsToMake.EXISTING_PORT;
                }
                if (nodeData.wiresToConnect != null || nodeData.wireToInsertOn != null) {
                    if (connectionsToMake != ConnectionsToMake.NONE) {
                        LDLib2.LOGGER.error("Cannot create node on wires and existing port at the same time.");
                        continue;
                    }

                    connectionsToMake = nodeData.wiresToConnect != null
                            ? ConnectionsToMake.EXISTING_WIRES
                            : ConnectionsToMake.INSERT_ON_WIRE;
                }

                var uid = nodeData.uid == null ? UUID.randomUUID() : nodeData.uid;

                // Create new element
                GraphElementModel createdElement = null;

                if (nodeData.nodeLibraryItem != null) {
                    // If there is a port, try to create a variable from the variable creation infos
                    createdElement = nodeData.nodeLibraryItem.createNode(
                            new GraphNodeCreationData(graphModel, nodeData.position(), SpawnFlags.DEFAULT, uid)
                    );
                }

                if (createdElement != null) {
                    elementsToSelect.add(createdElement);
                }

                List<WireModel> wiresToDelete = new ArrayList<>();
                var portNodeModel = (createdElement instanceof PortNodeModel p) ? p : null;
                switch (connectionsToMake) {
                    case NONE -> {}
                    case EXISTING_PORT -> {
                        var existingPortToConnect = nodeData.portModel;
                        var portToConnectItem = nodeData.nodeLibraryItem == null
                                ? null
                                : (nodeData.nodeLibraryItem.getData() instanceof NodeItemLibraryData data ? data.portToConnect() : null);
                        var newPortToConnect = portToConnectItem == null
                                ? (portNodeModel == null ? null : portNodeModel.getPortFitToConnectTo(existingPortToConnect))
                                : (portNodeModel == null ? null : portNodeModel.getPorts().stream()
                                .filter(p -> p.getUniqueName().equals(portToConnectItem.getUniqueName()))
                                .findFirst()
                                .orElse(null));

                        if (newPortToConnect != null) {
                            // Old wires to delete
                            wiresToDelete = WireCommands.getDropWireModelsToDelete(nodeData.portModel, null, null);

                            WireModel newWire;
                            if (existingPortToConnect.getDirection() == PortDirection.OUTPUT) {
                                newWire = graphModel.createWire(newPortToConnect, existingPortToConnect);
                            } else {
                                newWire = graphModel.createWire(existingPortToConnect, newPortToConnect);
                            }

                            elementsToSelect.add(newWire);
                        }
                    }
                    case ConnectionsToMake.INSERT_ON_WIRE -> {
                        if (portNodeModel instanceof InputOutputPortsNodeModel newModelToConnect) {
                            var wireInput = nodeData.wireToInsertOn.getToPort();
                            var wireOutput = nodeData.wireToInsertOn.getFromPort();

                            // Old wire to delete
                            wiresToDelete.add(nodeData.wireToInsertOn);

                            // connect input port
                            var inputPortModel = newModelToConnect.getPortFitToConnectTo(wireOutput);
                            var inputWire = inputPortModel == null ? null : graphModel.createWire(inputPortModel, wireOutput);

                            // connect output port
                            var outputPortModel = newModelToConnect.getPortFitToConnectTo(wireInput);
                            var outputWire = outputPortModel == null ? null : graphModel.createWire(wireInput, outputPortModel);
                        }
                    }
                    case ConnectionsToMake.EXISTING_WIRES -> {
                        for (var wire : nodeData.wiresToConnect) {
                            var wireModel = wire.left();
                            var wireSide = wire.right();
                            var portToConnect = nodeData.nodeLibraryItem == null ? null : (
                                    nodeData.nodeLibraryItem.getData() instanceof NodeItemLibraryData data ? data.portToConnect() : null
                                    );

                            var newPort = portToConnect == null
                                    ? (portNodeModel == null ? null : portNodeModel.getPortFitToConnectTo(wireModel.getOtherPort(wireSide)))
                                    : (portNodeModel == null ? null : portNodeModel.getPorts().stream()
                                    .filter(p -> p.getUniqueName().equals(portToConnect.getUniqueName()))
                                    .findFirst()
                                    .orElse(null));


                            WireModel newWire = null;
                            if (newPort != null) {
                                // Old wires to delete
                                wiresToDelete.addAll(WireCommands.getDropWireModelsToDelete(wireModel.getOtherPort(wireSide), null, List.of(wireModel)));

                                if (wireModel instanceof IGhostWireModel) {
                                    if (wireSide == WireSide.TO) {
                                        if (graphModel.getModel(wireModel.getFromPortUid()) != null) {
                                            newWire = graphModel.createWire(newPort, wireModel.getFromPort());
                                        }
                                    } else {
                                        if (graphModel.getModel(wireModel.getToPortUid()) != null) {
                                            newWire = graphModel.createWire(wireModel.getToPort(), newPort);
                                        }
                                    }
                                } else {
                                    wireModel.setPort(wireSide, newPort);
                                }
                            }
                        }
                    }
                }
                if (!wiresToDelete.isEmpty()) {
                    graphModel.deleteWires(wiresToDelete);
                }
            }
            if (!elementsToSelect.isEmpty()) {
                // selection
            }
            creationData.clear();
        }

        @Override
        public void undo() {

        }
    }
}
