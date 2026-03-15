package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortCapacity;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.WirePortalModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.IGhostWireModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireSide;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.*;

public final class WireCommands {
    public static List<WireModel> getDropWireModelsToDelete(PortModel portModel,
                                                            @Nullable Pair<WireSide, List<WireModel>> wiresToMove,
                                                            @Nullable List<WireModel> exceptWires){
        if (portModel == null)
            return Collections.emptyList();

        var sideToMove = wiresToMove == null ? null : wiresToMove.left();
        var wires = wiresToMove == null ? null : wiresToMove.right();
        if (portModel.getPortCapacity() == PortCapacity.MULTIPLE && wires == null)
            return Collections.emptyList();

        var wireModelsToDelete = new ArrayList<WireModel>();

        var connectedWires = new ArrayList<>(portModel.getConnectedWires());

        if (portModel.getPortCapacity() != PortCapacity.MULTIPLE) {
            // if a wire is created on a sub port, then none of its parent should have a wire.
            var parentPort = portModel.getParentPort();
            while (parentPort != null) {
                connectedWires.addAll(parentPort.getConnectedWires());
                parentPort = parentPort.getParentPort();
            }

            // if a wire is created on a parent port, then none of its descendant ports should have a wire.
            recurseAddSubPortWires(connectedWires, portModel);
        }

        for (WireModel otherWire : connectedWires) {
            if (otherWire instanceof IGhostWireModel) continue;
            if (exceptWires != null && exceptWires.contains(otherWire)) continue;

            if (wires == null) {
                if (portModel.getPortCapacity() != PortCapacity.MULTIPLE)
                    wireModelsToDelete.add(otherWire);
            } else {
                for (WireModel wireToMove : wires) {
                    if (otherWire.getUid() == wireToMove.getUid())
                        break;

                    if (portModel.getPortCapacity() != PortCapacity.MULTIPLE) {
                        wireModelsToDelete.add(otherWire);
                        break;
                    }

                    if (!wireModelsToDelete.contains(otherWire)) {
                        if (sideToMove == WireSide.TO && otherWire.getFromPort() == wireToMove.getFromPort() && otherWire.getToPort() == portModel ||
                                sideToMove == WireSide.FROM && otherWire.getFromPort() == portModel && otherWire.getToPort() == wireToMove.getToPort()) {
                            wireModelsToDelete.add(otherWire);
                            break;
                        }
                    }
                }

            }
        }

        return wireModelsToDelete;
    }

    private static void recurseAddSubPortWires(List<WireModel> connectedWires, PortModel port) {
        for (var subPort : port.getSubPorts()) {
            connectedWires.addAll(subPort.getConnectedWires());
            recurseAddSubPortWires(connectedWires, subPort);
        }
    }

    public static class CreateWireCommand extends UndoableGraphCommand {
        private final static Component NAME = Component.translatable("graph.commands.create_wire");

        public PortModel toPortModel;
        public PortModel fromPortModel;

        /**
         * Creates a new wire command.
         * @param toPortModel Destination port.
         * @param fromPortModel Origin port.
         */
        public CreateWireCommand(PortModel toPortModel, PortModel fromPortModel) {
            this.toPortModel = toPortModel;
            this.fromPortModel = fromPortModel;
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }

        @Override
        public void execute() {
            var wiresToDelete = new ArrayList<>(getDropWireModelsToDelete(fromPortModel, null, null));
            wiresToDelete.addAll(getDropWireModelsToDelete(toPortModel, null, null));

            if (!wiresToDelete.isEmpty()) {
                graphModel.deleteWires(wiresToDelete);
            }

            graphModel.createWire(toPortModel, fromPortModel);
        }

    }

    public static class ConvertWiresToPortalsCommand extends UndoableGraphCommand {
        private final static Component NAME = Component.translatable("graph.commands.covert_wires_to_portals");


        public record PortalData(WireModel wire, Vector2f start, Vector2f end) {}

        /**
         * Data describing which wire to transform and the position of the portals.
         */
        public List<PortalData> wireData;

        public ConvertWiresToPortalsCommand(List<PortalData> wiresToConvert) {
            this.wireData = wiresToConvert;
        }

        @Override
        public Component getCommandName() {
            return NAME;
        }

        @Override
        public void execute() {
            var existingPortalEntries = new HashMap<PortModel, WirePortalModel>();
            var existingPortalExits = new HashMap<PortModel, List<WirePortalModel>>();

            for (var wireModel : wireData) {
                graphModel.createPortalsFromWire(
                        wireModel.wire,
                        wireModel.start.add(120, 0, new Vector2f()),
                        wireModel.end.add(-100, 0, new Vector2f()),
                        12, existingPortalEntries, existingPortalExits
                        );
            }
        }

    }
}
