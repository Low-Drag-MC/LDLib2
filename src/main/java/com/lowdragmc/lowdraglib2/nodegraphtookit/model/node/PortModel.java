package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldConstantConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.ITypeConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.utils.ReorderType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import com.mojang.serialization.DataResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Model representing a port on a node.
 *
 * <p>Ports are connection points on nodes that allow data or execution flow to pass between nodes.
 * Each port has a direction (input or output), a data type, and can be connected to other compatible ports.</p>
 */
public class PortModel extends GraphElementModel implements IPort, IHasDisplayName, IHasContextualMenuItems, IFieldConstantConfigurable {
    @Getter
    protected PortNodeModel nodeModel;
    @Getter
    protected String portId;
    protected @Nullable Component title;
    protected TypeHandle dataTypeHandle;
    @Getter
    protected PortType portType;
    @Getter
    protected PortDirection direction;
    @Getter
    protected PortOrientation orientation;
    @Nullable
    protected PortCapacity portCapacity;
    @Getter
    protected PortModelOptions options;
    @Getter @Nullable
    protected PortModel parentPort;
    @Getter
    protected final List<PortModel> subPorts = new ArrayList<>();
    @Getter(AccessLevel.PROTECTED) @Setter(AccessLevel.PROTECTED)
    protected Constant computedConstant;
    protected Tooltips tooltips = Tooltips.empty();
    @Getter @Setter
    protected boolean isExpandable = false;
    @Getter @Setter
    protected boolean isExpanded;

    // runtime
    @Nullable
    protected Type dataTypeCache;

    /**
     * Optional per-port configurator override. When non-null, this is used by
     * {@link #buildConfigurator} instead of resolving from {@code dataTypeHandle}. Set via
     * {@code IInputPortBuilder.withConfigurable} or {@code IOptionBuilder.withConfigurable}.
     * Not persisted — reapplied on every {@code defineNode} call.
     */
    @Setter @Nullable
    protected ITypeConfigurable customTypeConfigurable;
    /**
     * Per-port toggle for the inspector field. When {@code false}, {@link #buildConfigurator}
     * (inherited from {@link IFieldConstantConfigurable}) is a no-op. Reapplied on every
     * {@code defineNode} call, mirroring the {@link #customTypeConfigurable} lifecycle.
     */
    @Setter @Getter
    protected boolean configuratorEnabled = true;
    /**
     * Optional reflection field this port maps to. Surfaced by {@link #getValueField()} so the
     * default configurator accessor can read annotations such as {@code @ConfigNumber}. Not
     * persisted — reapplied on every {@code defineNode} call.
     */
    @Setter @Nullable
    protected Field valueField;
    /** Object owner paired with {@link #valueField} for reflective annotation access. */
    @Setter @Nullable
    protected Object valueOwer;

    public PortModel(PortNodeModel nodeModel,
                     PortDirection direction,
                     PortOrientation orientation,
                     String portId,
                     PortType portType,
                     TypeHandle dataTypeHandle,
                     PortModelOptions options,
                     @Nullable PortModel parentPort) {
        validateId(portId);
        var uid = computePortUid(nodeModel, direction, portId, portType, dataTypeHandle, parentPort);
        super.setUid(uid);
        this.portId = portId;

        this.nodeModel = nodeModel;
        this.parentPort = parentPort;

        this.direction = direction;
        this.orientation = orientation;
        this.portType = portType;
        this.dataTypeHandle = dataTypeHandle;

        this.options = options;

        setGraphModel(nodeModel.getGraphModel());
    }

    static void validateId(String portId) {
        if (portId.chars().anyMatch(c -> c == '.')) {
            throw new IllegalArgumentException("\".\" is not a valid character in port ids as it is used with sub ports.");
        }
    }

    static String computeUniqueName(String uniqueId, String parentPortUniqueName) {
        var uniqueName = uniqueId;
        if(parentPortUniqueName != null) {
            uniqueName = uniqueId + "." + parentPortUniqueName;
        }
        return uniqueName;
    }

    protected static UUID computePortUid(PortNodeModel nodeModel,
                                PortDirection direction,
                                String portId,
                                PortType portType,
                                TypeHandle dataType,
                                @Nullable PortModel parentPort) {
        return UUID.nameUUIDFromBytes((nodeModel.getUid() + "|" +
                direction + "|" +
                portId + "|" +
                portType + "|" +
                dataType + "|" +
                (parentPort == null ? "" : parentPort.getUid())).getBytes());
    }

    /**
     * Sets the unique port id of the port.
     *
     * @param portId the unique name
     */
    public void setPortId(String portId) {
        if (Objects.equals(portId, this.portId)) return;
        validateId(portId);
        var oldUniqueName = getUniqueName();
        this.portId = portId;
        onUniqueNameChanged(oldUniqueName, getUniqueName());
        if (this.graphModel != null) {
            this.graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    @Override
    public Component getTitle() {
        if (title == null) {
            return Component.translatable(portId);
        }
        return title;
    }

    @Override
    public void setTitle(Component title) {
        if (Objects.equals(title, this.title)) return;
        this.title = title;
        if (this.graphModel != null) {
            this.graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.STYLE);
        }
    }

    public void setOptions(PortModelOptions options) {
        if (this.options == options) return;
        this.options = options;
        if (this.graphModel != null) {
            this.graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    public void setPortType(PortType portType) {
        if (this.portType == portType) return;
        this.portType = portType;
        if (this.graphModel != null) {
            this.graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    public void setDirection(PortDirection direction) {
        if (this.direction == direction) return;
        var oldDirection = this.direction;
        this.direction = direction;
        OnDirectionChanged(oldDirection, direction);
        if (this.graphModel != null) {
            this.graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    public void setOrientation(PortOrientation orientation) {
        if (this.orientation == orientation) return;
        this.orientation = orientation;
        if (this.graphModel != null) {
            this.graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    public PortCapacity getPortCapacity() {
        if (portCapacity != null) return portCapacity;
        if (portType == PortType.STATE) return PortCapacity.MULTIPLE;
        return !dataTypeHandle.equals(TypeHandles.EXECUTION_FLOW) && direction == PortDirection.INPUT ? PortCapacity.SINGLE : PortCapacity.MULTIPLE;
    }

    public void setPortCapacity(PortCapacity portCapacity) {
        if (this.portCapacity == portCapacity) return;
        this.portCapacity = portCapacity;
        if (this.graphModel != null) {
            this.graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    // todo polymorphic
    /**
     * Whether the port is polymorphic and its currently selected type is
     */
    public boolean isAutomatic() {
        return false;
    }

    public boolean isPolymorphic() {
        return false;
    }

    public TypeHandle getDataTypeHandle() {
        // todo polymorphic
        return isAutomatic() ? null : dataTypeHandle;
    }

    public void setDataTypeHandle(TypeHandle dataTypeHandle) {
        if (Objects.equals(this.dataTypeHandle, dataTypeHandle)) return;
        this.dataTypeHandle = dataTypeHandle;
        this.dataTypeCache = null;
        if (isPolymorphic() && !isAscendable()) {
            // todo polymorphic
        }
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    public Type getPortDataType() {
        if (dataTypeCache == null) {
            var t = dataTypeHandle.resolve();
            dataTypeCache = t == void.class || t == Void.class ? TypeHandles.Unknown.class : t;
        }
        return dataTypeCache;
    }

    /**
     * Gets the ports connected to this port.
     * @return The ports connected to this port
     */
    public List<PortModel> getConnectedPorts() {
        if (graphModel == null) return Collections.emptyList();
        var wires = getConnectedWires();
        var results = new ArrayList<PortModel>();
        for (var wire : wires) {
            var port = wire.getFromPort() == this ? wire.getToPort() : wire.getFromPort();
            if (port != null) {
                results.add(port);
            }
        }
        return results;
    }


    /**
     * Tells if a port can be connected to this one, taking into account the polymorphic configuration
     */
    public boolean canConnectPort(PortModel port) {
        // not self
        if (port == this) return false;
        // check type
        if (dataTypeHandle.isAssignableFrom(port.dataTypeHandle)) return true;

        // polymorphic cases
        if (isAutomatic()) {
            // todo polymorphic
        }
        if (port.isAutomatic()) {
            // todo polymorphic
        }

        return false;
    }

    protected void onUniqueNameChanged(String oldUniqueName, String newUniqueName) {
        if (graphModel != null) {
            graphModel.getPortWireIndex().portUniqueNameChanged(this, oldUniqueName, newUniqueName);
        }
        if (nodeModel != null) {
            nodeModel.onPortUniqueNameChanged(this, oldUniqueName, newUniqueName);
        }

        if (!subPorts.isEmpty()) {
            for (var subPort : subPorts) {
                subPort.onUniqueNameChanged(computeUniqueName(subPort.portId, oldUniqueName), subPort.getUniqueName());
            }
        }
    }

    protected void OnDirectionChanged(PortDirection oldDirection, PortDirection newDirection) {
        if (graphModel != null) {
            graphModel.getPortWireIndex().portDirectionChanged(this, oldDirection, newDirection);
        }
    }

    public String getUniqueName() {
        return computeUniqueName(portId, parentPort == null ? null : parentPort.getUniqueName());
    }

    /**
     * The default tooltip for the port.
     * @return The default tooltip is "[name] [Input|Output] of type (friendly name of the port type)" for ports (e.g. "input of type float").
     */
    public Tooltips getDefaultTooltips() {
        return Tooltips.of(getTitle().copy().append(" (")
                .append(Component.literal(dataTypeHandle.getFriendlyName()).withColor(dataTypeHandle.getTypeColor()))
                .append(")")
        );
    }

    public Tooltips getTooltips() {
        if (tooltips.isEmpty()) return getDefaultTooltips();
        return tooltips;
    }

    public void setTooltips(Tooltips tooltips) {
        if (this.tooltips.equals(tooltips)) return;
        this.tooltips = tooltips;
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.STYLE);
        }
    }

    /**
     * Sets the node model that owns this port.
     *
     * @param nodeModel the node model
     */
    public void setNodeModel(PortNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    /**
     * Gets the wires connected to this port.
     *
     * @return unmodifiable list of connected wires
     */
    public List<WireModel> getConnectedWires() {
        return graphModel == null ? Collections.emptyList() : graphModel.getWiresForPort(this);
    }

    /**
     * Checks whether two ports are connected.
     * @param otherPort The second port.
     * @return True if there is at least one wire that connects the two ports.
     */
    public boolean isConnectedTo(PortModel otherPort) {
        if (graphModel == null) return false;
        var wires = getConnectedWires();
        for (var wire : wires) {
            if (wire.getToPort() == otherPort || wire.getFromPort() == otherPort) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether this port is equivalent to another port.
     */
    public boolean equivalent(PortModel otherPortModel) {
        if (otherPortModel == null)
            return false;

        return direction == otherPortModel.direction
                && nodeModel.getUid().equals(otherPortModel.getUid())
                && getUniqueName().equals(otherPortModel.getUniqueName());
    }

    public boolean hasReorderableWires() {
        return dataTypeHandle.equals(TypeHandles.EXECUTION_FLOW) && direction == PortDirection.OUTPUT && isConnected();
    }

    @Nullable
    public Constant getEmbeddedValue() {
        if (computedConstant != null) return computedConstant;
        if (direction == PortDirection.INPUT && nodeModel instanceof NodeModel node) {
            return node.getInputConstantsById().get(getUniqueName());
        }
        return null;
    }

    /**
     * Whether the port creates a default embedded constant.
     */
    public boolean createEmbeddedValueIfNeeded() {
        return portType == PortType.DEFAULT;
    }

    /**
     * Whether all ancestors of this port are expanded.
     */
    public boolean areAncestorsExpanded() {
        if (parentPort == null) return true;
        return parentPort.isExpanded && parentPort.areAncestorsExpanded();
    }

    /**
     * Changes the order of a wire among its siblings.
     * @param wireModel The wire to reorder.
     * @param reorderType the type of move to do.
     */
    public void reorderWire(WireModel wireModel, ReorderType reorderType) {
        if (graphModel != null) {
            graphModel.reorderWire(wireModel, reorderType);
        }
    }

    /**
     * Gets the order of a wire on this port
     * @param wireModel the wire to get the order of.
     * @return the index
     */
    public int getWireOrder(WireModel wireModel) {
        return getConnectedWires().indexOf(wireModel);
    }

    /**
     * Adds a sub port to this port.
     * Users of the NodeModel class must not use this method directly, but rely on {@link NodeModel#addSubPort} instead.
     * @param portModel The sub port to add.
     */
    public void addSubPort(PortModel portModel) {
        subPorts.add(portModel);
        portModel.parentPort = this;
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    /**
     * Clears all sub ports.
     */
    public void clearSubPorts() {
        if (subPorts.isEmpty()) return;
        subPorts.clear();
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    /**
     * Checks if this port is a descendant of the given port.
     */
    public boolean isDescendantOf(PortModel ancestor) {
        if (parentPort == ancestor) return true;
        if (parentPort == null) return false;
        return parentPort.isDescendantOf(ancestor);
    }


    /**
     * Called when this port gets connected to another port.
     *
     * @param otherPort the other port
     */
    public void onConnection(PortModel otherPort) {
        if (isAutomatic()) {
            // TODO polymorphic
        }
    }

    /**
     * Called when this port gets disconnected from another port.
     *
     * @param otherPort the other port
     */
    public void onDisconnection(PortModel otherPort) {
        if (isAutomatic()) {
            // TODO polymorphic
        }
    }

    /**
     * Updates the port data type after the polymorphic port handle has changed.
     */
    public void updateDataTypeHandler() {
        if (isPolymorphic()) {
            // TODO polymorphic
        }
    }

    @Override
    public void getConnectedPorts(List<IPort> ports) {
        ports.clear();
        applyOnAllConnectedPorts(p -> {
            ports.add(p);
            return true;
        });
    }

    @Override
    public boolean isConnected() {
        return !getConnectedPorts().isEmpty();
    }

    @Override
    public IPort getFirstConnectedPort() {
        AtomicReference<IPort> first = new AtomicReference<>();
        applyOnAllConnectedPorts(p -> {
            first.set(p);
            return false;
        });
        return first.get();
    }

    protected boolean applyOnAllConnectedPorts(Predicate<IPort> predicate) {
        var wires = getConnectedWires();

        for (WireModel wire : wires) {
            var port = wire.getToPort() == this ? wire.getFromPort() : wire.getToPort();
            if (port == null)
                return true;
            if (port.nodeModel instanceof WirePortalModel portal){
                var declaration = portal.getDeclarationModel();

                var nodes = port.getNodeModel() instanceof WirePortalEntryModel ? getGraphModel().getExitPortals(declaration) : getGraphModel().getEntryPortals(declaration);
                for (var otherNode : nodes) {
                    for (PortModel portalPort : otherNode.getPorts()) {
                        if (!portalPort.applyOnAllConnectedPorts(predicate))
                            return false;
                    }
                }
            } else {
                if (!predicate.test(port)) return false;
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return getPortId();
    }

    @Override
    public Type getDataType() {
        return dataTypeHandle.equals(TypeHandles.EXECUTION_FLOW) ? null : getPortDataType();
    }

    @Override
    public Component getDisplayName() {
        return getTitle();
    }

    @Override
    public <T> DataResult<T> tryGetValue(Type type) {
        var embeddedValue = getEmbeddedValue();
        if (embeddedValue == null || isConnected()) {
            return DataResult.error(() -> "Cannot get value of port " + getUniqueName() + " as it is connected or has no embedded value.");
        }
        return embeddedValue.tryGetValue(type);
    }

    @Override
    public List<ContextualMenuItem> getContextualMenuItems() {
        // TODO
        return Collections.emptyList();
    }

//    public static final List<ContextualMenuItem> PORT_MENU_ITEMS = List.of(
//            ContextualMenuHelpers.addNodeFromPortItem,
//            ContextualMenuHelpers.createVariableFromPortItem,
//            ContextualMenuHelpers.copyValueItem,
//            ContextualMenuHelpers.pasteValueItem,
//            ContextualMenuHelpers.disconnectAllWiresItem,
//            ContextualMenuHelpers.expandPortItem,
//            ContextualMenuHelpers.collapsePortItem
//    );

    @Override
    public String toString() {
        return nodeModel.getUid() + "|" +
                direction + "|" +
                portId + "|" +
                portType + "|" +
                dataTypeHandle + "|" +
                (parentPort == null ? "null" : parentPort.getUid());
    }

    @Override
    public @Nullable Constant getConfigurableConstant() {
        return getEmbeddedValue();
    }

    @Override
    public void onValueChanged() {
        if (this.graphModel != null) {
            this.graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
        }
    }

    @Override
    public @Nullable ITypeConfigurable getCustomTypeConfigurable() {
        return customTypeConfigurable;
    }

    @Override
    public java.lang.reflect.Field getValueField() {
        return valueField;
    }

    @Override
    public Object getValueOwer() {
        return valueOwer;
    }
}
