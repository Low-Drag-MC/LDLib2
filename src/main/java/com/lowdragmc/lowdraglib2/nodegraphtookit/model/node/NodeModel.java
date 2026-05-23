package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.CollapsibleInOutNodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.SubPortCustomConstant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.TypeConstant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.NodeDefinitionScope;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.SubPortDefinitionScope;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption.PORT_ID_PREFIX;

/**
 * Node model implementation for user-defined nodes.
 *
 * <p>This model backs a {@link Node} instance and provides the implementation for ports and options.</p>
 */
public abstract class NodeModel extends InputOutputPortsNodeModel implements INodeWithOptions {
    @Getter
    protected Map<String, Constant> inputConstantsById;
    @Getter
    protected PortInfos inputPortInfos;
    @Getter
    protected PortInfos outputPortInfos;

    // runtime
    @Getter
    protected boolean isCurrentlyDefiningNode = false;
    @Getter
    protected boolean nodeOptionConstantsMigrated;
    @Nullable
    private SubPortDefinitionScope<? extends NodeModel> subPortDefinitionScope;

    /**
     * Raw NBT tags captured by {@link #deserializeAdditionalNBT} so that constant values can be
     * re-decoded after {@link #defineNode()} runs and the builder's {@code initializationCallback}
     * has installed the per-port {@code customCodec} / {@code serializationEnabled} on each
     * Constant. Without this second pass, codec-encoded values from disk would never decode
     * (phase 1 has no codec context) and {@code withoutSerialization} ports would retain
     * the typeHandle default instead of the builder default.
     *
     * <p>Entries are removed as they are consumed in {@link #updateConstantForInput}, and the
     * whole map is cleared at the end of {@code defineNode} so subsequent in-session
     * {@code defineNode} calls don't re-apply stale tags.</p>
     */
    @Nullable
    protected transient Map<String, CompoundTag> pendingConstantTags;

    protected NodeModel() {
        inputPortInfos = new PortInfos();
        outputPortInfos = new PortInfos();
        inputConstantsById = new HashMap<>();
    }

    @Override
    public Tag serializeAdditionalNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        // Serialize inputConstantsById
        var constantsTag = new CompoundTag();
        for (var entry : inputConstantsById.entrySet()) {
            constantsTag.put(entry.getKey(), TypeConstant.serializeConstant(entry.getValue(), provider));
        }
        if (!constantsTag.isEmpty()) {
            tag.put("inputConstants", constantsTag);
        }
        return tag;
    }

    @Override
    public void deserializeAdditionalNBT(Tag tag, HolderLookup.Provider provider) {
        if (tag instanceof CompoundTag compound && compound.contains("inputConstants")) {
            var constantsTag = compound.getCompound("inputConstants");
            var allKeys = constantsTag.getAllKeys();
            if (allKeys.isEmpty()) return;
            // Phase 1 (legacy, best-effort): produce fresh constants so option values are
            // available to onDefinePorts. Phase 2 runs after defineNode and re-decodes only the
            // constants whose builder has codec / withoutSerialization / explicit default — for
            // stateless ports Phase 1's result is final (see updateConstantForInput).
            pendingConstantTags = new HashMap<>(allKeys.size() * 2);
            for (var key : allKeys) {
                var perConstantTag = constantsTag.getCompound(key);
                pendingConstantTags.put(key, perConstantTag);
                var constant = TypeConstant.deserializeConstant(perConstantTag, provider);
                if (constant != null) {
                    inputConstantsById.put(key, constant);
                }
            }
        }
    }

    /**
     * Clears all ports from this node.
     */
    public void clearPorts() {
        for (var portModel : inputPortInfos.portsById.values()) {
            if (graphModel != null) {
                graphModel.unregisterPort(portModel);
            }
        }
        for (var portModel : outputPortInfos.portsById.values()) {
            if (graphModel != null) {
                graphModel.unregisterPort(portModel);
            }
        }
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.GRAPH_TOPOLOGY);
        }

        inputPortInfos.clear();
        outputPortInfos.clear();
    }

    /**
     * Changes the node mode.
     * @param modeIndex the index of the mode to change to
     */
    public void changeMode(int modeIndex) {
        // todo node mode
    }

    /**
     * Creates a new {@link NodeDefinitionScope} instance which provides methods to instantiate ports/options on the nodes.
     */
    protected NodeDefinitionScope<? extends NodeModel> createNodeDefinitionScope() {
        return new NodeDefinitionScope<>(this);
    }

    @Override
    public void onCreateNode() {
        super.onCreateNode();
        defineNode();
    }

    /**
     * Defines the node by calling the node's definition methods.
     */
    public void defineNode() {
        isCurrentlyDefiningNode = true;
        onPreDefineNode();

        inputPortInfos.previousPorts = inputPortInfos.portsById;
        for (var nodeOption : nodeOptions) {
            inputPortInfos.previousPorts.add(nodeOption.portModel);
        }

        outputPortInfos.previousPorts = outputPortInfos.portsById;

        inputPortInfos.orderedVisiblePorts.clear();
        outputPortInfos.orderedVisiblePorts.clear();
        nodeOptions.clear();
        nodeOptionsById.clear();

        inputPortInfos.portsById = new OrderedPorts();
        outputPortInfos.portsById = new OrderedPorts();

        var nodeDefinitionScope = createNodeDefinitionScope();
        onDefineNode(nodeDefinitionScope);

        onDefineSubPorts(inputPortInfos, null);
        onDefineSubPorts(outputPortInfos, null);

        removeObsoleteNodeOptionPorts();
        removeObsoleteWiresAndConstants();

        // Drop any pending tags that defineNode didn't consume — e.g. saved ports that no longer
        // exist in the current node definition. Leaving them would cause stale re-decode on the
        // next in-session defineNode.
        pendingConstantTags = null;

        isCurrentlyDefiningNode = false;
    }

    protected void redefinePort(PortModel port) {
        // Redefines a single port and its sub ports.
        var portInfos = getPortInfos(port.getDirection());
        portInfos.previousPorts = new OrderedPorts(1);
        recursivelyRemoveSubPorts(portInfos.portsById, portInfos.previousPorts, port);

        portInfos.orderedVisiblePorts.clear();

        onDefineSubPorts(portInfos, port);

        removeObsoleteWiresAndConstants();
    }

    private void recursivelyRemoveSubPorts(OrderedPorts portsById, OrderedPorts previousPorts, PortModel portModel) {
        for (PortModel subPort : portModel.getSubPorts()) {
            portsById.remove(subPort);
            previousPorts.add(subPort);
            recursivelyRemoveSubPorts(portsById, previousPorts, subPort);
        }
    }

    /**
     * Called by {@link #defineNode()} before the port lists are modified.
     */
    protected void onPreDefineNode() {
    }

    protected abstract void onDefineNode(NodeDefinitionScope<? extends NodeModel> scope);

    /**
     * Creates a new {@link SubPortDefinitionScope} instance which provides methods to instantiate sub ports.
     */
    protected SubPortDefinitionScope<? extends NodeModel> createSubPortDefinition() {
        return new SubPortDefinitionScope<>(this);
    }

    protected void onDefineSubPorts(PortInfos portInfos, @Nullable PortModel singlePort) {
        if (graphModel == null) return;

        if (subPortDefinitionScope == null) {
            // reuse
            subPortDefinitionScope = createSubPortDefinition();
        }

        var currentList = new ArrayList<PortModel>();
        var nextList = new ArrayList<PortModel>();

        if (singlePort != null)
            nextList.add(singlePort);
        else
            nextList.addAll(portInfos.portsById.values());

        var portModels = nextList;

        while (!portModels.isEmpty()) {
            currentList.clear();
            subPortDefinitionScope.addedPorts = currentList;

            for (var port : portModels) {
                if (port.getOrientation() == PortOrientation.Horizontal && graphModel.canExpandPort(port)) {
                    port.setExpandable(true);
                    subPortDefinitionScope.parentPort = port;
                    subPortDefinitionScope.refreshMustSpecifySubPorts(); // must be called before ClearSubPorts
                    port.clearSubPorts();
                    graphModel.onDefineSubPorts(subPortDefinitionScope, port);

                    if (subPortDefinitionScope.isMustSpecifySubPorts() && port.getSubPorts().isEmpty()) {
                        LDLib2.LOGGER.error("Sub ports must be specified for port " + port.getUniqueName());
                    }
                    port.setExpanded((portInfos.expandedPortsById).containsKey(port.getUniqueName()));
                } else {
                    port.setExpandable(false);
                    port.clearSubPorts();
                    portInfos.expandedPortsById.remove(port.getUniqueName());
                }
            }
            portModels = currentList;

            var c = currentList;
            currentList = nextList;
            nextList = c;
        }

        //Add sub port in the ordered list at the right place.

        int start, end;

        if( singlePort != null) {
            start = portInfos.portsById.values().indexOf(singlePort);
            end = start + 1;
        } else {
            start = 0;
            end = portInfos.portsById.size();
        }

        for (int i = start; i < end ; ++i) {
            //note: this will add sub port recursively.
            var port = portInfos.portsById.get(i);
            if (!port.getSubPorts().isEmpty()) {
                portInfos.portsById.insertRange(i + 1, port.getSubPorts());
                end += port.getSubPorts().size();
            }
        }
    }

    protected void removeObsoleteNodeOptionPorts() {
        if (inputPortInfos.previousPorts == null) return;
        var removedNodeOptionPorts = new ArrayList<PortModel>();
        // options
        for (var previousInput : inputPortInfos.previousPorts.values()) {
            if (!previousInput.getOptions().hasFlag(PortModelOptions.NODE_OPTION))
                continue;

            if (nodeOptions.stream().noneMatch(o -> o.getPortModel() == previousInput)) {
                if (graphModel != null) {
                    graphModel.unregisterPort(previousInput);
                }
                removedNodeOptionPorts.add(previousInput);
            }
        }

        if (graphModel != null && !removedNodeOptionPorts.isEmpty()) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.GRAPH_TOPOLOGY);
            graphModel.getCurrentGraphChangeDescription().addDeletedModels(removedNodeOptionPorts);
        }
    }

    protected void removeObsoleteWiresAndConstants() {
        if (inputPortInfos.previousPorts == null) return;
        var removedPortModels = new ArrayList<PortModel>();
        for (var entry : inputPortInfos.previousPorts.entrySet()) {
            if (inputPortInfos.portsById.containsKey(entry.getKey())) continue;
            var portModel = entry.getValue();
            if (!portModel.getOptions().hasFlag(PortModelOptions.NODE_OPTION)
                    && !portModel.portType.equals(PortType.MISSING_PORT)) {
                disconnectPort(portModel);
                if (graphModel != null) {
                    graphModel.unregisterPort(portModel);
                }
                removedPortModels.add(portModel);
            } else if (portModel.portType.equals(PortType.MISSING_PORT) && !portModel.getConnectedWires().isEmpty()) {
                // Prevents added missing ports that aren't obsolete yet from being overwritten by newly instantiated ports in OnDefineNode().
                inputPortInfos.portsById.add(portModel);
            }
        }

        for (var entry : outputPortInfos.previousPorts.entrySet()) {
            if (outputPortInfos.portsById.containsKey(entry.getKey())) continue;
            var portModel = entry.getValue();
            if (!portModel.getOptions().hasFlag(PortModelOptions.NODE_OPTION)
                    && !portModel.portType.equals(PortType.MISSING_PORT)) {
                disconnectPort(portModel);
                if (graphModel != null) {
                    graphModel.unregisterPort(portModel);
                }
                removedPortModels.add(portModel);
            } else if (portModel.portType.equals(PortType.MISSING_PORT) && !portModel.getConnectedWires().isEmpty()) {
                // Prevents added missing ports that aren't obsolete yet from being overwritten by newly instantiated ports in OnDefineNode().
                outputPortInfos.portsById.add(portModel);
            }
        }

        if (graphModel != null && !removedPortModels.isEmpty()) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.GRAPH_TOPOLOGY);
            graphModel.getCurrentGraphChangeDescription().addDeletedModels(removedPortModels);
        }

        // remove input constants that aren't used
        var idsToDelete = inputConstantsById.keySet().stream()
                .filter(id -> !inputPortInfos.portsById.containsKey(id)
                        && nodeOptions.stream().noneMatch(o -> o.portModel.getUniqueName().equals(id)))
                .toList();
        for (var id : idsToDelete) {
            inputConstantsById.remove(id);
        }

        // remove expanded status for removed ports
        cleanupExpandedPortDictionary(inputPortInfos);
        cleanupExpandedPortDictionary(outputPortInfos);
    }

    private void cleanupExpandedPortDictionary(PortInfos portInfos) {
        var portsById = portInfos.portsById;
        for (var entry : portInfos.expandedPortsById.entrySet()) {
            var id = entry.getKey();
            if (portsById.containsKey(id)) continue;
            portInfos.expandedPortsById.remove(id);
        }
    }

    /**
     * Deletes all the wires connected to a given port.
     * @param portModel The port model to disconnect.
     */
    protected void disconnectPort(PortModel portModel) {
        if (graphModel != null) {
            var wireModels = graphModel.getWiresForPort(portModel);
            graphModel.deleteWires(wireModels);
        }
    }

    /**
     * Adds an input port to this node.
     */
    public PortModel addInputPort(String portId,
                             TypeHandle dataType,
                             @Nullable PortType portType,
                             @Nullable PortOrientation orientation,
                             @Nullable PortModelOptions options,
                             @Nullable Consumer<Constant> initializationCallback,
                             @Nullable Consumer<Object> setterAction) {
        if (options == null) options = PortModelOptions.NONE;
        if (orientation == null) orientation = PortOrientation.Horizontal;
        if (portType == null) portType = PortType.DEFAULT;
        if (!options.hasFlag(PortModelOptions.NODE_OPTION) && portId.startsWith(PORT_ID_PREFIX)) {
            throw new IllegalArgumentException("Port ID must not start with " + PORT_ID_PREFIX);
        }
        var portModel = reuseOrCreatePortModel(PortDirection.INPUT, orientation,
                portId, portType, dataType, options, inputPortInfos, null);
        updateConstantForInput(portModel, initializationCallback, setterAction);
        return portModel;
    }

    /**
     * Adds a new data input port with no connector on a node.
     */
    public PortModel addNoConnectorInputPort(String portId,
                                             TypeHandle dataType,
                                             @Nullable PortType portType,
                                             @Nullable PortOrientation orientation,
                                             @Nullable PortModelOptions options,
                                             @Nullable Consumer<Constant> initializationCallback,
                                             @Nullable Consumer<Object> setterAction) {
        var portModel = addInputPort(portId, dataType, portType, orientation, options, initializationCallback, setterAction);
        portModel.setPortCapacity(PortCapacity.NONE);
        return portModel;
    }

    /**
     * Adds an output port to this node.
     */
    public PortModel addOutputPort(String portId,
                                   TypeHandle dataType,
                                   @Nullable PortType portType,
                                   @Nullable PortOrientation orientation,
                                   @Nullable PortModelOptions options) {
        if (options == null) options = PortModelOptions.NONE;
        if (orientation == null) orientation = PortOrientation.Horizontal;
        if (portType == null) portType = PortType.DEFAULT;
        return reuseOrCreatePortModel(PortDirection.OUTPUT, orientation, portId, portType, dataType, options, outputPortInfos, null);
    }

    /**
     * Adds a new missing port on a node.
     */
    public PortModel addMissingPort(PortDirection direction,
                                    String portId,
                                    @Nullable PortOrientation orientation) {
        if (direction == PortDirection.INPUT) {
            return addInputPort(portId, TypeHandles.MISSING_PORT, PortType.MISSING_PORT, orientation, PortModelOptions.NO_EMBEDDED_CONSTANT, null, null);
        }
        return addOutputPort(portId, TypeHandles.MISSING_PORT, PortType.MISSING_PORT, orientation, PortModelOptions.NO_EMBEDDED_CONSTANT);
    }

    public PortModel addInputSubPort(PortModel parent,
                                     String portId,
                                     TypeHandle typeHandle,
                                     Supplier<Object> getter,
                                     Consumer<Object> setter,
                                     @Nullable PortModelOptions options) {
        return addInputSubPortWithDelegates(parent, portId, typeHandle, getter, setter, options);
    }

    protected PortModel addInputSubPortWithDelegates(PortModel parent,
                                                     String portId,
                                                     TypeHandle typeHandle,
                                                     Supplier<Object> getter,
                                                     Consumer<Object> setter,
                                                     @Nullable PortModelOptions options) {
        if (!parent.isExpandable()) {
            throw new IllegalArgumentException("Parent port " + parent.getUniqueName() + " must be expandable.");
        }

        if (parent.getDirection() == PortDirection.OUTPUT) {
            throw new IllegalArgumentException("Parent port " + parent.getUniqueName() + " of Port " + portId + " must be an input port.");
        }

        var port = addSubPort(parent, portId, typeHandle, options);
        if (port.getComputedConstant() instanceof SubPortCustomConstant spcc) {
            spcc.setGetter(getter);
            spcc.setSetter(setter);
        } else {
            port.setComputedConstant(new SubPortCustomConstant(port, getter, setter));
        }

        return port;
    }

    public PortModel addSubPort(PortModel parent,
                                   String portId,
                                   TypeHandle dataType,
                                   @Nullable PortModelOptions options) {
        var port = commonAddSubPort(parent, portId, dataType, options);
        port.setComputedConstant(null);
        return port;
    }

    protected PortModel commonAddSubPort(PortModel parent,
                                         String portId,
                                         TypeHandle dataType,
                                         @Nullable PortModelOptions options) {
        if (!parent.isExpandable()) {
            throw new IllegalArgumentException("Parent port " + parent.getUniqueName() + " must be expandable.");
        }

        var portInfos = getPortInfos(parent.getDirection());
        if (options == null) options = PortModelOptions.NONE;
        return reuseOrCreatePortModel(parent.getDirection(), parent.getOrientation(), portId, parent.getPortType(), dataType, options, portInfos, parent);
    }

    /**
     * Adds a node option to the node.
     */
    public NodeOption addNodeOption(String optionId,
                                    TypeHandle dataType,
                                    @Nullable Tooltips tooltip,
                                    boolean showInInspectorOnly,
                                    int order,
                                    @Nullable Consumer<Constant> initializationCallback,
                                    @Nullable Consumer<Object> setterAction) {
        if (dataType.equals(TypeHandles.UNKNOWN) || dataType.equals(TypeHandles.EXECUTION_FLOW) ||
                dataType.equals(TypeHandles.MISSING) || dataType.equals(TypeHandles.MISSING_PORT))
            throw new IllegalArgumentException("Invalid type for node option");

        var portId = PORT_ID_PREFIX + optionId;

        // Now constants for NodeOptions have NodeOption.k_OptionIdPrefix in their id. We need to migrate constants with no prefix to the new id.
        if (!isNodeOptionConstantsMigrated() && !inputConstantsById.containsKey(portId)) {
            if (inputConstantsById.remove(optionId) instanceof Constant oldConstant) {
                inputConstantsById.put(portId, oldConstant);
            }
        }

        // A node option consists in a no connector port with extra info. We add a prefix to avoid id conflicts with regular ports.
        var noConnectorPort = addNoConnectorInputPort(portId, dataType, PortType.DEFAULT, PortOrientation.Horizontal, PortModelOptions.NODE_OPTION, initializationCallback, setterAction);
        noConnectorPort.setTitle(Component.translatable(optionId));

        if (tooltip != null) {
            noConnectorPort.setTooltips(tooltip);
        }

        var nodeOption = new NodeOption(optionId, noConnectorPort, showInInspectorOnly, order);
        nodeOptions.add(nodeOption);
        nodeOptionsById.put(optionId, nodeOption);
        return nodeOption;
    }

    /**
     * Updates an input port's constant.
     *
     * @param inputPort               the port to update
     * @param initializationCallback  initialization method for the constant
     * @param setterAction            method called after the constant value changes
     */
    protected void updateConstantForInput(
            PortModel inputPort,
            @Nullable Consumer<Constant> initializationCallback,
            @Nullable Consumer<Object> setterAction
    ) {
        var id = inputPort.getUniqueName();
        if (inputPort.options.hasFlag(PortModelOptions.NO_EMBEDDED_CONSTANT)) {
            inputConstantsById.remove(id);
            if (graphModel != null) {
                graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.UNSPECIFIED);
                return;
            }
        }

        Constant newConstant = null;
        if (inputConstantsById.get(id) instanceof Constant existingConstant) {
            if (graphModel != null) {
                newConstant = graphModel.createConstantValue(inputPort.dataTypeHandle);
            }
            var portDefinitionType = newConstant != null ? newConstant.getType() : inputPort.dataTypeHandle.resolve();

            if (!existingConstant.isAssignableFrom(portDefinitionType)) {
                // incompatible type, remove constant
                inputConstantsById.remove(id);
                if (graphModel != null) {
                    graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.UNSPECIFIED);
                }
            } else {
                // reuse
                existingConstant.setOwner(inputPort);
                existingConstant.clearListeners();
                if (setterAction != null) {
                    existingConstant.addListener(setterAction);
                }
                // Phase 2 of constant deserialize: re-apply the builder's initializationCallback
                // (installs customCodec, serializationEnabled, defaultValue and resets value to
                // builder default) then re-decode the saved tag into the existing constant using
                // the now-installed codec. Without this, codec-encoded values from disk would
                // never decode (phase 1 has no codec) and withoutSerialization ports would keep
                // the typeHandle default instead of the builder default.
                //
                // Skipped when there's no pending tag — i.e., this is an in-session defineNode
                // call rather than the post-deserialize one. In that case the constant already
                // holds the live value and we'd otherwise clobber it with the builder default.
                if (pendingConstantTags != null && pendingConstantTags.containsKey(id)) {
                    // Skip Phase 2 entirely when the builder has no state worth applying — Phase 1's
                    // accessor decode (run during deserializeAdditionalNBT) already produced the right
                    // value and set the deserializeFailed flag if anything went wrong.
                    if (initializationCallback == null) {
                        pendingConstantTags.remove(id);
                    } else {
                        try {
                            initializationCallback.accept(existingConstant);
                        } catch (Exception e) {
                            LDLib2.LOGGER.error("Builder initializationCallback threw for port {} of node {}", id, this.getClass().getSimpleName(), e);
                        }
                        var pendingTag = pendingConstantTags.remove(id);
                        TypeConstant.deserializeIntoConstant(existingConstant, pendingTag);
                    }
                }
                return;
            }
        }

        // create new constant if needed
        if (graphModel != null && inputPort.createEmbeddedValueIfNeeded() && !inputPort.getDataTypeHandle().equals(TypeHandles.UNKNOWN)) {
            newConstant = graphModel.createConstantValue(inputPort.dataTypeHandle);
            if (newConstant != null) {
                newConstant.setOwner(inputPort);
                if (initializationCallback != null) {
                    initializationCallback.accept(newConstant);
                }
                newConstant.clearListeners();
                if (setterAction != null) {
                    newConstant.addListener(setterAction);
                }
                inputConstantsById.put(id, newConstant);
                graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.UNSPECIFIED);
            }
        }
    }

    /**
     * Searches for a reusable port in the previous ports or the GraphModel. If a reusable port is found, it is returned. Otherwise, null is returned.
     * On return, the port must have the passed direction, type and data type.
     * @return A port that has the passed direction, type and data type.
     */
    @Nullable
    public PortModel getReusablePort(@Nullable OrderedPorts ports,
                                     PortDirection direction,
                                     String portId,
                                     PortType portType,
                                     TypeHandle dataType,
                                     @Nullable PortModel parentPort) {
        var uid = PortModel.computePortUid(this, direction, portId, portType, dataType, parentPort);
        // found a port from graph
        PortModel portModel = null;
        if (graphModel != null && graphModel.getModel(uid) instanceof PortModel found) {
            portModel = found;
        } else if (ports != null && ports.get(PortModel.computeUniqueName(portId, parentPort == null ? null : parentPort.getUniqueName())) instanceof PortModel found) {
            portModel = found;
        }
        if (portModel != null) {
            portModel.setTitle(Component.translatable(portId));
            portModel.setDataTypeHandle(dataType);
            portModel.setPortType(portType);
            return portModel;
        }
        return null;
    }


    /**
     * Reuses an existing port model if a reusable one matching the provided parameters is found.
     * Otherwise, a new port model is created with the given options and parameters. The method also
     * handles registering the port model with the graph model and establishing any required
     * parent-child relationships for hierarchical ports.
     * @return The reused or newly created {@link PortModel} instance.
     */
    protected PortModel reuseOrCreatePortModel(PortDirection direction,
                                               PortOrientation orientation,
                                               String portId,
                                               PortType portType,
                                               TypeHandle dataType,
                                               PortModelOptions options,
                                               PortInfos portInfos,
                                               @Nullable PortModel parentPort) {
        // If a port is added outside onDefineNode, clear the visible ports list to force a rebuild. ( Case of missing ports )
        if (!isCurrentlyDefiningNode) {
            getPortInfos(direction).orderedVisiblePorts.clear();
        }
        // reuse existing ports when ids match, otherwise add port

        var portModelToAdd = getReusablePort(portInfos.previousPorts, direction, portId, portType, dataType, parentPort);
        if (portModelToAdd != null) {
            portModelToAdd.setOptions(options);
        } else {
            portModelToAdd = createPort(direction, orientation, portId, portType, dataType, options, parentPort);
            if (graphModel != null) {
                graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.GRAPH_TOPOLOGY);
                graphModel.getCurrentGraphChangeDescription().addNewModel(portModelToAdd);
            }
        }
        if (graphModel != null) {
            graphModel.registerPort(portModelToAdd);
        }
        if (parentPort != null) {
            parentPort.addSubPort(portModelToAdd);
        }

        if (!options.hasFlag(PortModelOptions.NODE_OPTION) && portModelToAdd.getParentPort() == null) {
            portInfos.portsById.add(portModelToAdd);
        }

        return portModelToAdd;
    }

    /**
     * Creates a new port on the node.
     */
    protected PortModel createPort(PortDirection direction,
                                   PortOrientation orientation,
                                   String portId,
                                   PortType portType,
                                   TypeHandle dataType,
                                   PortModelOptions options,
                                   @Nullable PortModel parentPort) {
        return new PortModel(this, direction, orientation, portId, portType, dataType, options, parentPort);
    }

    public PortInfos getPortInfos(PortDirection direction) {
        return direction == PortDirection.INPUT ? inputPortInfos : outputPortInfos;
    }

    @Override
    public void onPortUniqueNameChanged(PortModel portModel, String oldUniqueName, String newUniqueName) {
        if (portModel.getDirection() == PortDirection.INPUT) {
            inputPortInfos.portsById.changePortName(portModel, oldUniqueName);
            if (inputConstantsById.remove(oldUniqueName) instanceof Constant constant) {
                if (!inputConstantsById.containsKey(newUniqueName)) {
                    inputConstantsById.put(newUniqueName, constant);
                }
            }
            if (inputPortInfos.expandedPortsById.remove(oldUniqueName) instanceof PortModel expandedPort) {
                inputPortInfos.expandedPortsById.putIfAbsent(newUniqueName, expandedPort);
            }
        } else {
            outputPortInfos.portsById.changePortName(portModel, oldUniqueName);

            if (outputPortInfos.expandedPortsById.remove(oldUniqueName) instanceof PortModel expandedPort) {
                outputPortInfos.expandedPortsById.put(newUniqueName, expandedPort);
            }
        }

        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.UNSPECIFIED);
        }
    }

    @Override
    public boolean removeUnusedMissingPort(PortModel portModel) {
        if (graphModel == null || portModel.getPortType() != PortType.MISSING_PORT || !portModel.getConnectedWires().isEmpty())
            return false;

        graphModel.unregisterPort(portModel);
        graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.GRAPH_TOPOLOGY);
        return getPortInfos(portModel.getDirection()).portsById.remove(portModel);
    }

    @Override
    public void onConnection(PortModel selfConnectedPortModel, PortModel otherConnectedPortModel) {
        super.onConnection(selfConnectedPortModel, otherConnectedPortModel);
        getPortInfos(selfConnectedPortModel.getDirection()).orderedVisiblePorts.clear();
    }

    @Override
    public void onDisconnection(PortModel selfConnectedPortModel, PortModel otherConnectedPortModel) {
        super.onDisconnection(selfConnectedPortModel, otherConnectedPortModel);
        // If the disconnected port had one of its ancestor collapsed, it was only visible because it was connected.
        if (!selfConnectedPortModel.areAncestorsExpanded()) {
            getPortInfos(selfConnectedPortModel.getDirection()).orderedVisiblePorts.clear();
            if (graphModel != null) {
                graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.DATA);
            }
        }
    }

    @Override
    protected void onPortDataTypeChanged(PortModel portModel, TypeHandle previousType, TypeHandle dataTypeHandle) {
        if (!isCurrentlyDefiningNode) {
            redefinePort(portModel);
        }
        updateConstantForInput(portModel, null, null);
    }

    @Override
    public Map<String, PortModel> getInputsById() {
        return inputPortInfos.portsById.getDictionary();
    }

    @Override
    public Map<String, PortModel> getOutputsById() {
        return outputPortInfos.portsById.getDictionary();
    }

    @Override
    public List<PortModel> getInputsByDisplayOrder() {
        return inputPortInfos.portsById.values();
    }

    @Override
    public List<PortModel> getOutputsByDisplayOrder() {
        return outputPortInfos.portsById.values();
    }

    @Override
    public List<PortModel> getVisibleInputsByDisplayOrder() {
        buildVisiblePorts(inputPortInfos);
        return inputPortInfos.orderedVisiblePorts;
    }

    @Override
    public List<PortModel> getVisibleOutputsByDisplayOrder() {
        buildVisiblePorts(outputPortInfos);
        return outputPortInfos.orderedVisiblePorts;
    }

    protected void buildVisiblePorts(PortInfos portInfos) {
        if (portInfos.orderedVisiblePorts.isEmpty() && !portInfos.portsById.isEmpty()) {
            for (var port : portInfos.portsById.values()) {
                if (port.getParentPort() == null || port.areAncestorsExpanded() || port.isConnected())
                    portInfos.orderedVisiblePorts.add(port);
            }
        }
    }

    @Override
    public boolean isAllowSelfConnect() {
        return false;
    }

    @Override
    public IGuiTexture getNodeIcon() {
        return IGuiTexture.EMPTY;
    }

    @Override
    public boolean hasNodePreview() {
        return false;
    }

    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new CollapsibleInOutNodeElement(this);
    }
}
