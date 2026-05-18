package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for a model of a node that has ports.
 */
public abstract class PortNodeModel extends AbstractNodeModel {
    /**
     * {@inheritDoc}
     *
     * Dependent models include the base dependent models and all port models.
     */
    @Override
    public Stream<GraphElementModel> getDependentModels() {
        return Stream.concat(super.getDependentModels(), getPorts().stream());
    }

    /**
     * Retrieves all port models of this node.
     *
     * @return the port models
     */
    public abstract Collection<PortModel> getPorts();

    /**
     * Retrieves the ports of a node that satisfy the requested direction and type.
     *
     * @param direction the direction of the ports to retrieve
     * @param portType  the type of the ports to retrieve
     * @return the ports that satisfy the requested direction and type
     */
    public List<PortModel> getPorts(PortDirection direction, PortType portType) {
        return getPorts().stream()
            .filter(p ->
                p.getPortType() == portType && (p.getDirection().mask & direction.mask) == direction.mask
            )
            .toList();
    }

    /**
     * Called when any port on this node model gets connected.
     *
     * @param selfConnectedPortModel  the port on this node that got connected
     * @param otherConnectedPortModel the port on the other node
     */
    public void onConnection(PortModel selfConnectedPortModel, PortModel otherConnectedPortModel) {
        selfConnectedPortModel.onConnection(otherConnectedPortModel);
    }

    /**
     * Called when any port on this node model gets disconnected.
     *
     * @param selfConnectedPortModel  the port on this node that got disconnected
     * @param otherConnectedPortModel the port on the other node
     */
    public void onDisconnection(PortModel selfConnectedPortModel, PortModel otherConnectedPortModel) {
        selfConnectedPortModel.onDisconnection(otherConnectedPortModel);
    }

    /**
     * Called when the unique name of any port on this node model has changed.
     *
     * @param portModel     the port model
     * @param oldUniqueName the old unique name of the port
     * @param newUniqueName the new unique name of the port
     */
    public abstract void onPortUniqueNameChanged(
        PortModel portModel,
        String oldUniqueName,
        String newUniqueName
    );

    /**
     *Updates an input port's constant.
     * @param inputPort the port to update
     * @param initializationCallback the callback to initialize the constant with
     * @param setterAction the action to perform when the constant changes
     */
    protected abstract void updateConstantForInput(
            PortModel inputPort,
            @Nullable Consumer<Constant> initializationCallback,
            @Nullable Consumer<Object> setterAction
    );

    /**
     * Gets the model of a port that would be fit to connect to another port model.
     *
     * @param portModel the port model we want to connect to this node
     * @return a compatible port model, or {@code null} if none was found
     */
    public abstract PortModel getPortFitToConnectTo(PortModel portModel);

    /**
     * Removes a missing port when it is no longer used.
     *
     * @param portModel the port to remove
     * @return {@code true} if the missing port was removed, {@code false} otherwise
     */
    public abstract boolean removeUnusedMissingPort(PortModel portModel);

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WireModel> getConnectedWires() {
        if (getGraphModel() == null) {
            return Collections.emptyList();
        }

        return getPorts().stream()
            .flatMap(p -> getGraphModel().getWiresForPort(p).stream())
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Called when the data type of a port has changed.
     *
     * @param portModel       the port model
     * @param previousType   the previous type
     * @param dataTypeHandle the new data type
     */
    protected abstract void onPortDataTypeChanged(
        PortModel portModel,
        TypeHandle previousType,
        TypeHandle dataTypeHandle
    );

    /**
     * Gets the offset for wire connection from this port.
     * @return the offset value
     */
    public float getPortWireOffset() {
        return 15;
    }
}
