package com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.WireElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Capabilities;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IGraphElementUIModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Model representing a wire connection between two ports.
 *
 * <p>A wire connects an output port to an input port, allowing data or execution flow to pass between nodes.
 * Each wire has exactly two endpoints: a "from" port (typically output) and a "to" port (typically input).</p>
 */
public class WireModel extends GraphElementModel implements IPortWireIndexModel, IGraphElementUIModel {
    @Getter @Nullable
    private PortModel fromPort;
    @Getter @Nullable
    private PortModel toPort;
    @Getter @Setter @Nullable
    private Component bubbleText;

    public WireModel() {
        capabilities.addAll(List.of(
                Capabilities.DELETABLE,
                Capabilities.COPIABLE,
                Capabilities.SELECTABLE,
                Capabilities.ASCENDABLE
        ));
    }

    public UUID getFromPortUid() {
        return getFromPort() == null ? null : getFromPort().getUid();
    }

    public UUID getToPortUid() {
        return getToPort() == null ? null : getToPort().getUid();
    }

    @Override
    public void setGraphModel(GraphModel value) {
        super.setGraphModel(value);
        // todo reference
    }

    public void setFromPort(PortModel fromPort) {
        var oldPort = this.fromPort;
        if (oldPort != null && oldPort.getNodeModel() != null) {
            oldPort.getNodeModel().removeUnusedMissingPort(oldPort);
        }
        this.fromPort = fromPort;
        onPortChanged(oldPort, fromPort);
    }

    public void setToPort(PortModel toPort) {
        var oldPort = this.toPort;
        if (oldPort != null && oldPort.getNodeModel() != null) {
            oldPort.getNodeModel().removeUnusedMissingPort(oldPort);
        }
        this.toPort = toPort;
        onPortChanged(oldPort, toPort);
    }

    protected void onPortChanged(PortModel oldPort, PortModel newPort) {
        if (graphModel != null) {
            graphModel.updateWire(this, oldPort, newPort);
        }
    }

    /**
     * Sets the endpoints of the wire.
     */
    public void setPorts(PortModel toPortModel, PortModel fromPortModel) {
        if (toPortModel == null || fromPortModel == null
                || toPortModel.getNodeModel() == null ||  fromPortModel.getNodeModel() == null) return;

        var oldFromPort = this.fromPort;
        var oldToPort = this.toPort;

        setFromPort(fromPortModel);
        setToPort(toPortModel);

        var fromIsDifferent = oldFromPort != fromPortModel;
        var toIsDifferent = oldToPort != toPortModel;
        var needOnConnection = fromIsDifferent || toIsDifferent;

        if (oldFromPort != null && oldToPort != null) {
            if (fromIsDifferent) {
                oldFromPort.getNodeModel().onDisconnection(oldFromPort, oldToPort);
            }

            if (toIsDifferent) {
                oldToPort.getNodeModel().onDisconnection(oldToPort, oldFromPort);
            }
        }

        // If either ports have changed, both end need to know about the new connection.
        if (needOnConnection) {
            toPortModel.getNodeModel().onConnection(toPortModel, fromPortModel);
            fromPortModel.getNodeModel().onConnection(fromPortModel, toPortModel);
        }
    }

    /**
     * Sets the port of the wire on a specific side.
     */
    public void setPort(WireSide side, @Nullable PortModel value) {
        PortModel oldPort;
        PortModel otherPort;
        if (side == WireSide.FROM) {
            if (value == getFromPort())
                return;
            oldPort = getFromPort();
            otherPort = getToPort();
            setFromPort(value);
        } else {
            if (value == getToPort())
                return;
            oldPort = getToPort();
            otherPort = getFromPort();
            setToPort(value);
        }

        // All ports must not be null to call OnConnection/OnDisconnection
        if (otherPort == null)
            return;

        if (oldPort != null) {
            oldPort.getNodeModel().onDisconnection(oldPort, otherPort);
        }

        if (value != null) {
            // Both end of the wire need to know about the new connection.
            value.getNodeModel().onConnection(value, otherPort);
            otherPort.getNodeModel().onConnection(otherPort, value);
        }

        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.UNSPECIFIED);
        }
    }

    public PortModel getPort(WireSide side) {
        return side == WireSide.FROM ? getFromPort() : getToPort();
    }

    public PortModel getOtherPort(WireSide side) {
        return side == WireSide.FROM ? getToPort() : getFromPort();
    }

    public record AddMissingPortResult(PortMigrationResult result, @Nullable AbstractNodeModel nodeModel) {}

    /**
     * Creates missing ports in the case where the original ports are missing.
     * @return A migration result pair for the input and output port migration.
     */
    public Pair<AddMissingPortResult, AddMissingPortResult> addMissingPorts() {
        PortMigrationResult inputResult = null;
        PortMigrationResult outputResult = null;
        AbstractNodeModel inputNode = null;
        AbstractNodeModel outputNode = null;
        // todo missing
//        if (getToPort() == null) {
//            inputResult = m_ToPortReference.AddMissingPort(PortDirection.Input, m_ToPortReference.PortOrientation) ?
//                    PortMigrationResult.MISSING_PORT_ADDED : PortMigrationResult.MISSING_PORT_FAILURE;
//
//            inputNode = m_ToPortReference.NodeModel;
//        } else {
//            inputResult = PortMigrationResult.MISSING_PORT_NOT_NEEDED;
//        }
//
//        if (getFromPort() == null) {
//            outputResult = m_FromPortReference.AddMissingPort(PortDirection.Output, m_FromPortReference.PortOrientation) ?
//                    PortMigrationResult.MISSING_PORT_ADDED : PortMigrationResult.MISSING_PORT_FAILURE;
//
//            outputNode = m_FromPortReference.NodeModel;
//        } else {
//            outputResult = PortMigrationResult.MISSING_PORT_NOT_NEEDED;
//        }

        return Pair.of(new AddMissingPortResult(inputResult, inputNode), new AddMissingPortResult(outputResult, outputNode));
    }

    @Override
    public Tag serializeAdditionalNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        if (fromPort != null) tag.putUUID("fromPortUid", fromPort.getUid());
        if (toPort != null) tag.putUUID("toPortUid", toPort.getUid());
        return tag;
    }

    @Override
    public void deserializeAdditionalNBT(Tag tag, HolderLookup.Provider provider) {
        // Port references are resolved by GraphModel after all nodes are created.
        // Store the UUIDs temporarily in the tag - GraphModel will read them.
    }

    /**
     * Gets the serialized from-port UUID from the additional NBT tag.
     * Used by GraphModel during deserialization to resolve port references.
     */
    public static @Nullable UUID getFromPortUidFromTag(CompoundTag tag) {
        if (tag.contains("_additional")) {
            var additional = tag.getCompound("_additional");
            if (additional.contains("fromPortUid")) return additional.getUUID("fromPortUid");
        }
        return null;
    }

    /**
     * Gets the serialized to-port UUID from the additional NBT tag.
     * Used by GraphModel during deserialization to resolve port references.
     */
    public static @Nullable UUID getToPortUidFromTag(CompoundTag tag) {
        if (tag.contains("_additional")) {
            var additional = tag.getCompound("_additional");
            if (additional.contains("toPortUid")) return additional.getUUID("toPortUid");
        }
        return null;
    }

    @Override
    public @Nullable WireElement createElementUI() {
        return new WireElement(this);
    }

    @Override
    public String toString() {
        return "Wire(" + getFromPort() + "->" + getToPort() + ")";
    }
}
