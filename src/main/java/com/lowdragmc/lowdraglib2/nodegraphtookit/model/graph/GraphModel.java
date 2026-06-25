package com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphLogger;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortCapacity;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.IGraphReferenceResolver;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.IGraphCommand;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.utils.ReorderType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.TypeConstant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModelBase;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.IGroupItemModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.SectionModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.SubPortDefinitionScope;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.PlacematModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.StickyNoteModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WirePlaceHolder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireSide;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The model that represents a graph's structure and contents.
 *
 * <p>GraphModel manages nodes, wires, variables, and other graph elements.
 * It also tracks changes for efficient UI updates.</p>
 */
public abstract class GraphModel extends GraphElementModel implements IGraphElementContainer {
    public final static String DEFAULT_SECTION_NAME = "";
    @Getter
    private List<AbstractNodeModel> nodeModels;
    @Getter
    private List<WireModel> wireModels;
    @Getter
    private List<PlacematModel> placematModels;
    @Getter
    private List<StickyNoteModel> stickyNoteModels;
    @Getter @Nullable
    private List<GraphModel> subGraphs;
    @Getter @Nullable
    private List<GraphModel> localSubGraphs;
    @Getter
    private List<DeclarationModel> portalModels;
    @Nullable
    private PortWireIndex<WireModel> portWireIndex;
    @Getter
    private List<IPlaceHolder> placeholders;
    @Getter
    private List<SectionModel> sectionModels;
    @Getter
    private List<VariableDeclarationModelBase> graphVariableModels;

    // runtime
    private GraphChangeDescription currentChangeDescription = new GraphChangeDescription();
    @Nullable
    private Map<UUID, GraphElementModel> elementsByUID;
    @Getter
    private Map<UUID, PlaceholderData> placeholderData;
    @Getter
    private Set<String> existingVariableNames;
    /**
     * Runtime parent pointer for local subgraphs — set when this GraphModel is added to a parent's
     * {@link #localSubGraphs}. Not persisted; rebuilt during deserialization.
     */
    @Getter @Nullable
    private GraphModel parentGraph;
    /**
     * Runtime context plugged in by the editor: provides external graph resolution and similar
     * services that the pure model layer should not depend on directly. Null outside an editor.
     */
    @Getter
    @Nullable
    private IGraphReferenceResolver referenceResolver;

    /**
     * Creates a new graph model.
     */
    protected GraphModel() {
        nodeModels = new ArrayList<>();
        wireModels = new ArrayList<>();
        placematModels = new ArrayList<>();
        stickyNoteModels = new ArrayList<>();
        placeholders = new ArrayList<>();
        portalModels= new ArrayList<>();
        sectionModels = new ArrayList<>();
        graphVariableModels = new ArrayList<>();

        existingVariableNames = new HashSet<>();
        placeholderData = new HashMap<>();

        // todo move to CleanupSections
        createSection(DEFAULT_SECTION_NAME);
    }

    public @NotNull PortWireIndex<WireModel> getPortWireIndex() {
        if (portWireIndex == null) {
            portWireIndex = new PortWireIndex<>(wireModels);
        }
        return portWireIndex;
    }

    /**
     * Retrieves a list of supported {@link Node} classes for this implementation.
     *
     * @return a list of {@code Class} objects representing the supported {@link Node} types.
     */
    public abstract List<Class<? extends Node>> getSupportNodes();

    /**
     * Retrieves a list of supported type handles. It will be used to create constant and variable declarations.
     *
     * @return a {@code List} of {@link TypeHandle} objects representing the supported types.
     */
    public abstract List<TypeHandle> getSupportTypes();

    /**
     * Retrieves node classes shown in the item library.
     */
    public List<Class<? extends Node>> getLibrarySupportNodes() {
        return getSupportNodes();
    }

    /**
     * Retrieves type handles shown as constant nodes in the item library.
     */
    public List<TypeHandle> getLibrarySupportTypes() {
        return getSupportTypes();
    }

    /**
     * Retrieves type handles shown when creating or editing blackboard variables.
     */
    public List<TypeHandle> getVariableSupportTypes() {
        return getSupportTypes();
    }

    /**
     * Retrieves the variable kinds that may be exposed as ports when this graph is used as a subgraph.
     */
    public Set<VariableKind> getSupportedSubgraphVariableKinds() {
        return Set.of(VariableKind.INPUT, VariableKind.OUTPUT);
    }

    public boolean supportsSubgraphVariableKind(VariableKind kind) {
        return getSupportedSubgraphVariableKinds().contains(kind);
    }

    /**
     * Clamps variable modifier flags to the subgraph exposure directions supported by this graph.
     */
    public ModifierFlags sanitizeSubgraphVariableModifiers(@Nullable ModifierFlags flags) {
        if (flags == null || flags == ModifierFlags.NONE) return ModifierFlags.NONE;

        boolean read = flags.hasFlag(ModifierFlags.READ) && supportsSubgraphVariableKind(VariableKind.INPUT);
        boolean write = flags.hasFlag(ModifierFlags.WRITE) && supportsSubgraphVariableKind(VariableKind.OUTPUT);

        if (read && write) return ModifierFlags.READ_WRITE;
        if (read) return ModifierFlags.READ;
        if (write) return ModifierFlags.WRITE;
        return ModifierFlags.NONE;
    }

    /**
     * Whether it is allowed to create {@link WirePortalModel} and add them to the graph.
     */
    public boolean allowPortalCreation() {
        return true;
    }

    /**
     * Whether it is allowed to create sub-graphs.
     */
    public boolean allowSubgraphCreation() {
        return !isStateMachineGraph();
    }

    /**
     * Whether it is allowed to create {@link VariableDeclarationModelBase} and add them to the graph.
     */
    public boolean allowExposedVariableCreation() {
        return true;
    }

    /**
     * Whether to hide the ports editor when the port is connected. Default is true.
     */
    public boolean hideConnectedPortsEditor() {
        return true;
    }

    /**
     * Whether vertical input ports show their embedded constant editor (configurator) while
     * unconnected. Default is {@code false}: vertical ports are typically compact top/bottom
     * connectors with no inline value field. Override to opt back in.
     */
    public boolean showVerticalPortConfigurator() {
        return false;
    }

    /**
     * Whether the graph is a state machine graph.
     */
    public boolean isStateMachineGraph() {
        return false;
    }

    /**
     * Vetoes an editor command before it executes; default {@code true} (allow). Consulted by
     * {@code GraphView.dispatchCommand}. {@link CustomGraphModelImpl} delegates to
     * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph#canExecuteCommand}.
     */
    public boolean canExecuteCommand(IGraphCommand command) {
        return true;
    }

    /**
     * Called after an editor command has executed; default no-op. {@link CustomGraphModelImpl}
     * delegates to {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph#onCommandExecuted}.
     */
    public void onCommandExecuted(IGraphCommand command) {
    }

    /**
     * Called after the editor's current graph state has been loaded or refreshed. Implementations
     * may emit validation diagnostics into {@code logger}.
     */
    public void onGraphChanged(GraphLogger logger) {
    }

    /**
     * Gets all wires connected to a specific port.
     *
     * @param port the port
     * @return list of connected wires
     */
    public List<WireModel> getWiresForPort(PortModel port) {
        return getPortWireIndex().getWiresForPort(port);
    }

    // ----------------------------
    // Change tracking
    // ----------------------------
    /**
     * Gets the current change description for this graph.
     *
     * @return the change description
     */
    public GraphChangeDescription getCurrentGraphChangeDescription() {
        return currentChangeDescription;
    }

    /**
     * Clears the current change description and returns it.
     *
     * @return the previous change description
     */
    public GraphChangeDescription flushChanges() {
        GraphChangeDescription result = currentChangeDescription;
        currentChangeDescription = new GraphChangeDescription();
        return result;
    }

    // ----------------------------
    // IGraphElementContainer
    // ----------------------------
    @Override
    public List<GraphElementModel> getGraphElementModels() {
        return getElementsByUID().values().stream().filter(m -> m.getContainer() == this).toList();
    }

    @Override
    public void removeContainerElements(Collection<? extends GraphElementModel> elementsToRemove) {
        removeElements(elementsToRemove);
    }

    @Override
    public boolean repair() {
        return false;
    }

    /**
     * Checks whether the graph is a Container Graph or not. If it is not a Container Graph, it is an Asset Graph.
     * <br>
     * A Container Graph is a graph that cannot be nested inside another graph, and can be referenced by a game object or scene.
     * @return True if the graph is a container graph, false otherwise.
     */
    public boolean isContainerGraph() {
        return false;
    }

    /**
     * Checks the conditions to specify whether the Asset Graph can be a subgraph or not.
     */
    public boolean canBeSubGraph() {
        return !isContainerGraph();
    }

    /**
     * Retrieves a list of ports from the given candidates that are compatible with the specified port.
     * A port is considered compatible if both ports can connect to each other using
     * {@code PortModel#canConnectTo}.
     *
     * @param candidates the list of {@code PortModel} instances to filter for compatibility.
     * @param portModel the {@code PortModel} to check compatibility against.
     * @return a {@code List<PortModel>} containing all compatible ports from the candidates.
     */
    public List<PortModel> getCompatiblePorts(List<PortModel> candidates, PortModel portModel) {
        return candidates.stream().filter(candidate -> isCompatiblePort(portModel, candidate)).toList();
    }

    /**
     * Indicates whether a given type handle from a port can be assigned to another type handle from a port.
     * @param destination The destination port to which we want to assign type handle.
     * @param source The source port from which we want to assign type handle.
     * @return Whether a given port's data handle can be assigned to another port's type handle.
     */
    public boolean canAssignTo(PortModel destination, PortModel source) {
        return destination.canConnectPort(source);
    }

    /**
     * Gets all ports in the graph.
     */
    public Stream<PortModel> getPortModels() {
        return getElementsByUID().values().stream().filter(PortModel.class::isInstance).map(PortModel.class::cast);
    }

    /**
     * Determines whether two ports can be connected together by a wire.
     * @param startPortModel The port from which the wire would come from.
     * @param compatiblePortModel The port to which the wire would go to.
     * @return True if the two ports can be connected. False otherwise.
     */
    public boolean isCompatiblePort(PortModel startPortModel, PortModel compatiblePortModel) {
        if (startPortModel.getPortCapacity() == PortCapacity.NONE || compatiblePortModel.getPortCapacity() == PortCapacity.NONE)
            return false;

        var startWirePortalModel = startPortModel.getNodeModel() instanceof WirePortalModel portalModel ? portalModel : null;

        if (startPortModel.getPortType() != compatiblePortModel.getPortType())
            return false;

        if (startPortModel.getPortType() == PortType.MISSING_PORT || compatiblePortModel.getPortType() == PortType.MISSING_PORT)
            return false;

        // No good if ports belong to same node that does not allow self connect
        if (compatiblePortModel == startPortModel ||
                (compatiblePortModel.getNodeModel() != null || startPortModel.getNodeModel() != null) &&
                        !startPortModel.getNodeModel().isAllowSelfConnect() && compatiblePortModel.getNodeModel() == startPortModel.getNodeModel())
            return false;

        // No good if it's on the same portal either.
        if (compatiblePortModel.getNodeModel() instanceof WirePortalModel wirePortalModel) {
            if (wirePortalModel.getDeclarationModel().getUid().equals(startWirePortalModel == null ? null : startWirePortalModel.getUid()))
                return false;
        }

        // This is true for all ports
        if (compatiblePortModel.getDirection() == startPortModel.getDirection() ||
                compatiblePortModel.getPortType() != startPortModel.getPortType())
            return false;

        if (startPortModel.getDirection() == PortDirection.OUTPUT)
            return canAssignTo(compatiblePortModel, startPortModel);
        return canAssignTo(startPortModel, compatiblePortModel);
    }

    public void setGraphObjectDirty() {
        // todo
    }

    /**
     * Changes the order of a wire among its siblings.
     */
    public void reorderWire(WireModel wireModel, ReorderType reorderType) {
        var fromPort = wireModel.getFromPort();
        if (fromPort != null && fromPort.hasReorderableWires()){
            if (portWireIndex != null) {
                portWireIndex.wireReordered(wireModel, reorderType);
            }
            applyReorderToGraph(fromPort);

            var siblingWires = fromPort.getConnectedWires();
            getCurrentGraphChangeDescription().addChangedModels(siblingWires, ChangeHint.GRAPH_TOPOLOGY);
            getCurrentGraphChangeDescription().addChangedModel(fromPort, ChangeHint.GRAPH_TOPOLOGY);
        }
    }

    /**
     * Reorders {@link #wireModels} after the {@link #portWireIndex} is updated.
     */
    protected void applyReorderToGraph(PortModel portModel) {
        var orderedList = getWiresForPort(portModel);
        if (orderedList.isEmpty()) return;
        // How this works:
        // graph has wires [A, B, C, D, E, F] and [B, D, E] are reorderable wires
        // say D has been moved to first place by a user
        // reorderable wires have been reordered as [D, B, E]
        // find indices for any of (D, B, E) in the graph: [1, 3, 4]
        // place [D, B, E] at those indices, we get [A, D, C, B, E, F]

        var indices = new ArrayList<Integer>();

        // find the indices of every wire potentially affected by the reorder
        for (int i = 0; i < wireModels.size(); i++) {
            if (orderedList.contains(wireModels.get(i)))
                indices.add(i);
        }

        // When duplicating wires, it may happen that the new wire (present in orderedList) is not yet part of WireModels.
        // If so, we can't reorder the wires yet.
        if (indices.size() < orderedList.size())
            return;

        // place every reordered wire at an index that is part of the collection.
        for (int i = 0; i < orderedList.size(); i++) {
            wireModels.set(indices.get(i), orderedList.get(i));
        }

        setGraphObjectDirty();
    }

    // region registration

    /**
     * Gets a map of all elements in the graph, indexed by their unique identifier (UID).
     */
    @NotNull
    protected Map<UUID, GraphElementModel> getElementsByUID() {
        if (elementsByUID == null) {
            buildElementsByUID();
        }
        return elementsByUID;
    }

    protected void buildElementsByUID() {
        elementsByUID = new HashMap<>();
        nodeModels.forEach(this::registerElement);
        wireModels.forEach(this::registerElement);
        stickyNoteModels.forEach(this::registerElement);
        placematModels.forEach(this::registerElement);
        // Some variables may not be under any section.
        graphVariableModels.forEach(this::registerElement);
        portalModels.forEach(this::registerElement);
        sectionModels.forEach(this::registerElement);
    }

    /**
     * Registers an element so that the GraphModel can find it through its UID.
     * @param model The element to register.
     */
    protected void registerElement(GraphElementModel model) {
        if (model == null) return;
        var prev = getElementsByUID().putIfAbsent(model.getUid(), model);
        if (prev != null && prev != model && !(model instanceof IPlaceHolder)) {
            LDLib2.LOGGER.error("Duplicate element UID: {}", model.getUid());
        }

        model.getDependentModels().forEach(this::registerElement);
    }

    public boolean hasModel(UUID uid) {
        return getElementsByUID().containsKey(uid);
    }

    public GraphElementModel getModel(UUID uid) {
        return getElementsByUID().get(uid);
    }

    /**
     * Unregisters an element from the GraphModel.
     * @param model The element to unregister.
     */
    protected void unregisterElement(GraphElementModel model) {
        getElementsByUID().remove(model.getUid());
        model.getDependentModels().forEach(this::unregisterElement);
    }

    public void registerPort(PortModel portModel) {
        if (portModel.getNodeModel() == null || !portModel.getNodeModel().getSpawnFlags().isOrphan()) {
            registerElement(portModel);
        }
    }

    public void unregisterPort(PortModel portModel) {
        if (portModel.getNodeModel() == null || !portModel.getNodeModel().getSpawnFlags().isOrphan()) {
            unregisterElement(portModel);
        }
    }

    /**
     * Registers a block node (and recursively its ports) in the graph's UID map. Called by
     * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel#insertBlock}
     * after the block has been attached to its parent context. Blocks are <em>not</em> added
     * to {@link #nodeModels} — they remain reachable only through their parent context.
     */
    public void registerBlockNode(com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel block) {
        if (block == null) return;
        registerElement(block);
    }

    /**
     * Unregisters a block node (and recursively its ports) from the graph's UID map.
     */
    public void unregisterBlockNode(com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel block) {
        if (block == null) return;
        unregisterElement(block);
    }

    /**
     * Registers a node preview model.
     *
     * @param previewModel the preview model
     */
    public void registerNodePreview(NodePreviewModel previewModel) {
//        if (previewModel != null && !nodePreviewModels.contains(previewModel)) {
//            nodePreviewModels.add(previewModel);
//            previewModel.setGraphModel(this);
//        }
    }

    /**
     * Unregisters a node preview model.
     *
     * @param previewModel the preview model
     * @return {@code true} if removed
     */
    public boolean unregisterNodePreview(NodePreviewModel previewModel) {
//        return nodePreviewModels.remove(previewModel);
        return false;
    }

    /**
     * Deletes graph element models in the graph.
     */
    public void deleteElements(List<? extends GraphElementModel> graphElementModels) {
        var initialVariables = getGraphVariableModels().stream()
                .filter(v -> v != null && v.isInputOrOutput()).collect(Collectors.toSet());
        var elementsByType = new ElementsByType(graphElementModels);

        // Add nodes that would be backed by declaration models.
        elementsByType.variableDeclarationsModels.stream()
                .flatMap(d -> findReferencesInGraph(AbstractNodeModel.class, d).stream())
                .forEach(elementsByType.nodeModels::add);

        // Add wires connected to the deleted nodes.
        var allWires = new HashSet<>(wireModels);
        for (var placeholder : placeholders) {
            if (placeholder instanceof WireModel wireModel) {
                allWires.add(wireModel);
            }
        }
        for (var node : elementsByType.nodeModels) {
            if (!(node instanceof PortNodeModel portNode)) continue;

            for (var portModel : portNode.getPorts()) {
                for (WireModel wire : allWires) {
                    if (wire != null && (wire.getToPort() == portModel || wire.getFromPort() == portModel)) {
                        elementsByType.wireModels.add(wire);
                    }
                }
            }
        }

        deleteVariableDeclarations(elementsByType.variableDeclarationsModels, false);
        deleteGroups(elementsByType.groupModels);
        deleteStickyNotes(elementsByType.stickyNoteModels);
        deletePlacemats(elementsByType.placematModels);
        deleteWires(elementsByType.wireModels);
        deleteNodes(elementsByType.nodeModels, false, true);

        if (!elementsByType.variableDeclarationsModels.isEmpty()) {
            // Find out if there were any deleted I/O variable declaration.
            for (VariableDeclarationModelBase variableDeclaration : getGraphVariableModels()) {
                if (variableDeclaration != null && variableDeclaration.isInputOrOutput()) {
                    initialVariables.remove(variableDeclaration);
                }
            }

            if (!initialVariables.isEmpty()) {
                // todo sub graph
//                for (var recursiveSubgraphNode : getSelfReferringSubgraphNodes())
//                    recursiveSubgraphNode.update();
            }
        }
//
//        foreach (var statePortModel in statePortModels)
//        {
//            statePortModel.UpdateAllOffsets();
//        }
    }

    /**
     * Removes elements from the lists of graph element models of the graph intertnal.
     * <br/>
     * To delete elements from the graph, call {@link #deleteElements} instead
     * @param elements
     */
    protected void removeElements(Collection<? extends GraphElementModel> elements) {
        for (var element : elements) {
            switch (element) {
                case IPlaceHolder placeHolder:
                    removePlaceholder(placeHolder);
                    break;
                case StickyNoteModel stickyNoteModel:
                    removeStickyNote(stickyNoteModel);
                    break;
                case PlacematModel placematModel:
                    removePlacemat(placematModel);
                    break;
                case VariableDeclarationModelBase variableDeclarationModel:
                    removeVariableDeclaration(variableDeclarationModel);
                    break;
                case WireModel wireModel:
                    removeWire(wireModel);
                    break;
                case com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel blockNodeModel:
                    // Blocks live inside a context, not in the top-level nodeModels list.
                    // Route through the parent so its block list and wires stay consistent.
                    if (blockNodeModel.getContextNodeModel() != null) {
                        blockNodeModel.getContextNodeModel().removeBlock(blockNodeModel);
                    } else {
                        unregisterBlockNode(blockNodeModel);
                    }
                    break;
                case AbstractNodeModel nodeModel:
                    removeNode(nodeModel);
                    break;
                case PortModel portModel:
                    unregisterPort(portModel);
                    break;
                case SectionModel sectionModel:
                    removeSection(sectionModel);
                    break;
                case GroupModel groupModel:
                    removeGroup(groupModel);
                    break;
                default:
                    unregisterElement(element);
                    break;
            }
        }
    }

    // endregion

    /**
     * Creates a constant of the type represented by type
     * @param dataTypeHandle the type handle
     * @return the created constant
     */
    public Constant createConstantValue(TypeHandle dataTypeHandle) {
//        if (dataTypeHandle.isCustomTypeHandle()) return null;
        var t = dataTypeHandle.resolve();
        if (t == void.class || t == Void.class) return null;

        var instance = new TypeConstant();
        instance.init(dataTypeHandle);
        return instance;
    }

    /**
     * Gets the constant type associated with the given
     * @param typeHandle the handle for which to retrieve the type.
     * @return the type associated with typeHandle
     */
    @Nullable
    public Class<? extends Constant> getConstantType(TypeHandle typeHandle) {
//        if (typeHandle.isCustomTypeHandle()) return null;
        var t = typeHandle.resolve();
        if (t == void.class || t == Void.class) return null;
        return TypeConstant.class;
    }

    protected ConstantNodeModel newConstantNodeModel() {
        return new ConstantNodeModel();
    }

    /**
     * Indicates whether a given port can be expanded and have sub ports.
     */
    public boolean canExpandPort(PortModel port) {
        return false;
    }

    /**
     * Defines the sub ports of a given port, if {@link #canExpandPort} returns true.
     * @param subPortDefinitionScope the definition of the sub ports.
     * @param port the port
     */
    public void onDefineSubPorts(SubPortDefinitionScope<? extends NodeModel> subPortDefinitionScope, PortModel port) {

    }

    // region node

    protected Class<? extends WirePortalEntryModel> getWirePortalEntryType() {
        return WirePortalEntryModel.class;
    }

    protected Class<? extends WirePortalExitModel> getWirePortalExitType() {
        return WirePortalExitModel.class;
    }

    public <T extends AbstractNodeModel> T createNodeWithType(Class<T> nodeType,
                                                      String nodeName,
                                                      Vector2f position,
                                                      @Nullable UUID uid,
                                                      @Nullable Consumer<T> initializationCallback,
                                                      @Nullable SpawnFlags spawnFlags) {
        Consumer<AbstractNodeModel> setupWrapper = null;
        if (initializationCallback != null) {
            setupWrapper = n -> initializationCallback.accept((T) n);
        }
        return (T) createNode(nodeType, nodeName, position, uid, setupWrapper, spawnFlags);
    }

    public AbstractNodeModel createNode(Class<?> nodeType,
                                        String nodeName,
                                        Vector2f position,
                                        @Nullable UUID uid,
                                        @Nullable Consumer<AbstractNodeModel> initializationCallback,
                                        @Nullable SpawnFlags spawnFlags) {
        if (!allowPortalCreation() && WirePortalModel.class.isAssignableFrom(nodeType)) {
            throw new IllegalArgumentException("Wire portal creation is disabled.");
        }

        if (!allowPortalCreation() && SubgraphNodeModel.class.isAssignableFrom(nodeType)) {
            throw new IllegalArgumentException("Subgraph node creation is disabled.");
        }

        if (spawnFlags == null) spawnFlags = SpawnFlags.NONE;
        var nodeModel = instantiateNode(nodeType, nodeName, position, uid, initializationCallback, spawnFlags);

        if (!spawnFlags.isOrphan() && nodeModel.getContainer() == this) {
            addNode(nodeModel);
        }
        return nodeModel;
    }

    public ConstantNodeModel createConstantNode(TypeHandle constantType,
                                                String constantName,
                                                Vector2f position,
                                                @Nullable UUID uid,
                                                @Nullable Consumer<ConstantNodeModel> initializationCallback,
                                                @Nullable SpawnFlags spawnFlags) {
        if (spawnFlags == null) spawnFlags = SpawnFlags.NONE;
        return (ConstantNodeModel) createNode(getConstantType(constantType), constantName, position, uid, n -> {
            if (n instanceof ConstantNodeModel nodeModel) {
                nodeModel.getConstant().init(constantType);
                if (initializationCallback != null) initializationCallback.accept(nodeModel);
            }
        }, spawnFlags);
    }

    /**
     * Indicates whether a variable is allowed in the graph or not.
     * @param variable The variable in the graph.
     * @param graphModel The graph of the variable.
     * @return {@code true} if the variable is allowed in the graph.
     */
    public boolean canCreateVariableNode(VariableDeclarationModelBase variable, GraphModel graphModel) {
        // todo does it necessary?
//        var allowMultipleDataOutputInstances = allowMultipleDataOutputInstances() != AllowMultipleDataOutputInstances.Disallow;
        return variable.getDataTypeHandle().equals(TypeHandles.EXECUTION_FLOW)
                || variable.getModifiers() != ModifierFlags.WRITE
                || graphModel.findReferencesInGraph(VariableNodeModel.class, variable).isEmpty();
    }

    protected Class<? extends VariableNodeModel> getVariableNodeType() {
        return VariableNodeModel.class;
    }

    public VariableNodeModel createVariableNode(VariableDeclarationModelBase declarationModel,
                                                Vector2f position,
                                                @Nullable UUID uid,
                                                @Nullable SpawnFlags spawnFlags) {
        var nodeType = getVariableNodeType();

        Consumer<AbstractNodeModel> initializationCallback = n -> {
            if (n instanceof VariableNodeModel variableNodeModel) {
                variableNodeModel.setDeclarationModel(declarationModel);
            }
        };

        spawnFlags = spawnFlags == null ? SpawnFlags.DEFAULT : spawnFlags;
        return (VariableNodeModel) createNode(nodeType, declarationModel.getName(), position, uid, initializationCallback, spawnFlags);
    }

    /**
     * Instantiates a node with uid.
     */
    protected AbstractNodeModel instantiateNode(Class<?> nodeType,
                                                String nodeName,
                                                Vector2f position,
                                                @Nullable UUID uid,
                                                @Nullable Consumer<AbstractNodeModel> initializationCallback,
                                                @Nullable SpawnFlags spawnFlags) {
        if (nodeType == null) throw new IllegalArgumentException("nodeType cannot be null");
        if (!allowPortalCreation() && WirePortalModel.class.isAssignableFrom(nodeType)) {
            throw new IllegalArgumentException("Wire portal creation is disabled.");
        }

        if (!allowPortalCreation() && SubgraphNodeModel.class.isAssignableFrom(nodeType)) {
            throw new IllegalArgumentException("Subgraph node creation is disabled.");
        }

        AbstractNodeModel nodeModel;
        if (Constant.class.isAssignableFrom(nodeType)) {
            Constant constant;
            try {
                constant = (Constant) nodeType.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate constant of type " + nodeType.getName(), e);
            }
            var constantNodeModel = newConstantNodeModel();
            constantNodeModel.setConstant(constant);
            nodeModel = constantNodeModel;
        } else if (AbstractNodeModel.class.isAssignableFrom(nodeType)) {
            try {
                nodeModel = (AbstractNodeModel) nodeType.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate node model of type " + nodeType.getName(), e);
            }
        } else throw new IllegalArgumentException("nodeType must be a subclass of AbstractNodeModel");

        nodeModel.setGraphModel(this);
        nodeModel.setName(nodeName);

        if (spawnFlags == null) spawnFlags = SpawnFlags.NONE;
        nodeModel.setSpawnFlags(spawnFlags);
        nodeModel.setPosition(position);
        if (uid != null) nodeModel.setUid(uid);
        if (initializationCallback != null) {
            initializationCallback.accept(nodeModel);
        }
        nodeModel.onCreateNode();
        return nodeModel;
    }

    /**
     * Deletes a node from the graph.
     */
    public void deleteNode(AbstractNodeModel nodeToDelete, boolean deleteConnections, boolean deleteUnrefPortalDeclarations) {
        deleteNodes(Collections.singletonList(nodeToDelete), deleteConnections, deleteUnrefPortalDeclarations);
    }

    public void deleteNodes(Collection<? extends AbstractNodeModel> nodeModels, boolean deleteConnections, boolean deleteUnrefPortalDeclarations) {
        List<WirePortalModel> portalRefs = new ArrayList<>();
        var deletedElementsByContainer = new HashMap<IGraphElementContainer, List<GraphElementModel>>();

        for (var nodeModel : nodeModels) {
            if (nodeModel.isDeletable()) {
                deletedElementsByContainer.computeIfAbsent(nodeModel.getContainer(), k -> new ArrayList<>())
                        .add(nodeModel);

                if (deleteConnections) {
                    deleteWires(nodeModel.getConnectedWires());
                }

                // If all the portals with the given declaration are deleted, delete the declaration.
                if (deleteUnrefPortalDeclarations && nodeModel instanceof WirePortalModel wirePortalModel
                        && wirePortalModel.getDeclarationModel() != null) {
                    portalRefs = findReferencesInGraph(WirePortalModel.class, wirePortalModel.getDeclarationModel());
                    portalRefs.removeIf(nodeModels::contains);

                    if (portalRefs.isEmpty()) {
                        if (wirePortalModel.getDeclarationModel() instanceof PortalDeclarationPlaceholder placeholderModel) {
                            removePlaceholder(placeholderModel);
                        } else {
                            removePortal(wirePortalModel.getDeclarationModel());
                        }
                    }
                }

                if (nodeModel instanceof SubgraphNodeModel subgraphNodeModel
                        && subgraphNodeModel.isReferencingLocalSubgraph()){
                    removeLocalSubgraph(subgraphNodeModel.getSubgraphModel());
                }

                nodeModel.onDeleteNode();
            }
        }

        for (var entry : deletedElementsByContainer.entrySet()) {
            var container = entry.getKey();
            var elements = entry.getValue();
            if (container instanceof GraphModel gm && gm.getUid().equals(getUid())) {
                removeElements(elements);
            } else {
                // todo container
//                container.removeContainerElements(elements);
            }
        }
    }

    /**
     * Adds a node to the graph.
     */
    protected void addNode(AbstractNodeModel nodeModel) {
        if (!allowPortalCreation() && nodeModel instanceof WirePortalModel){
            throw new IllegalArgumentException("Wire portal creation is disabled.");

        }

        if (!allowSubgraphCreation() && nodeModel instanceof SubgraphNodeModel) {
            throw new IllegalArgumentException("Subgraph node creation is disabled.");
        }

        // todo shall we keep it?
        if (nodeModel.needsContainer())
            throw new IllegalArgumentException("Node cannot be added to graph because it needs a container.");

        registerElement(nodeModel);
        // todo meta data
//        AddMetaData(nodeModel, m_GraphNodeModels.Count);
        nodeModels.add(nodeModel);

        getCurrentGraphChangeDescription().addNewModel(nodeModel);
    }

    /**
     * Removes a node model from the graph.
     */
    protected void removeNode(AbstractNodeModel nodeModel) {
        if (nodeModel == null) return;

        unregisterElement(nodeModel);

        var index = -1;
        for (int i = 0; i < nodeModels.size(); i++) {
            var model = nodeModels.get(i);
            if (model == null) continue;
            if (model.getUid().equals(nodeModel.getUid())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            // todo meta data
//            RemoveFromMetadata(indexToRemove, PlaceholderModelHelper.ModelToMissingTypeCategory(nodeModel));
            nodeModels.remove(index);
            nodeModels.add(index, null);
            currentChangeDescription.addDeletedModel(nodeModel);
        }
    }


    /// endregion

    // region Wire

    /**
     * Creates a wire connecting two ports.
     * This method creats a wire that connects two nodes,
     * originating from an output port and going to an input port. A unique identifier (UUID) is assigned to the newly created wire.
     *
     * @param fromPort The port from which the wire originates.
     * @param toPort The port that the wire connects to.
     * @param uid The unique identifier (UUID) to assign to the newly created item.
     * @return The newly created wire.
     */
    public WireModel createWire(PortModel toPort, PortModel fromPort, @Nullable UUID uid) {
        return createWire(WireModel.class, toPort, fromPort, false, uid);
    }

    public WireModel createWire(PortModel toPort, PortModel fromPort) {
        return createWire(toPort, fromPort, null);
    }

    /**
     * Creates a wire and adds it to the graph.
     */
    public WireModel createWire(Class<? extends WireModel> wireType, PortModel toPort, PortModel fromPort,
                                boolean reuseExisting, @Nullable UUID uid) {
        if (toPort != null && toPort.getDirection() == PortDirection.OUTPUT
                && fromPort != null && fromPort.getDirection() == PortDirection.INPUT) {
            // switch
            return createWire(wireType, fromPort, toPort, reuseExisting, uid);
        }

        if (reuseExisting) {
            var existing = getAnyWireConnectedToPorts(toPort, fromPort);
            if (existing != null)
                return existing;
        }

        var wireModel = instantiateWire(wireType, toPort, fromPort, uid);
        addWire(wireModel);

        return wireModel;
    }

    protected WireModel getAnyWireConnectedToPorts(PortModel toPort, PortModel fromPort) {
        var wires = getWiresForPort(toPort);
        for (var wire : wires) {
            if (wire.getToPort() == toPort && wire.getFromPort() == fromPort)
                return wire;
        }
        return null;
    }

    /**
     * Instantiates a wire with uid.
     */
    protected WireModel instantiateWire(Class<? extends WireModel> wireType, PortModel toPort, PortModel fromPort, @Nullable UUID uid) {
        try {
            var wireModel = wireType.getConstructor().newInstance();
            wireModel.setGraphModel(this);
            if (uid != null)
                wireModel.setUid(uid);
            wireModel.setPorts(toPort, fromPort);
            return wireModel;
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to instantiate wire of type {}", wireType.getName(), e);
            throw new RuntimeException("Failed to instantiate wire of type " + wireType.getName());
        }
    }

    protected void addWire(WireModel wireModel) {
        registerElement(wireModel);
        // todo meta
//        AddMetaData(wireModel, m_GraphWireModels.Count);
        wireModels.add(wireModel);
        if (portWireIndex != null) {
            portWireIndex.wireAdded(wireModel);
        }
        getCurrentGraphChangeDescription().addNewModel(wireModel);
    }

    protected void removeWire(WireModel wireModel) {
        if (wireModel != null) {
            unregisterElement(wireModel);
            var index = -1;
            for (int i = 0; i < wireModels.size(); i++) {
                var wire = wireModels.get(i);
                if (wire == null) continue;
                if (wire.getUid().equals(wireModel.getUid())) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                wireModels.remove(index);
                // todo meta
//                RemoveFromMetadata(indexToRemove, ManagedMissingTypeModelCategory.Wire);
                wireModels.add(index, null);
                currentChangeDescription.addDeletedModel(wireModel);
            }

            if (portWireIndex != null) {
                portWireIndex.wireRemoved(wireModel);
            }

            // Remove missing port with no connections.
            if (wireModel.getToPort() instanceof PortModel to && to.getPortType().equals(PortType.MISSING_PORT) && to.getConnectedWires().isEmpty()) {
                var nodeModel = to.getNodeModel();
                if (nodeModel != null) {
                    nodeModel.removeUnusedMissingPort(to);
                }
            }

            if (wireModel.getFromPort() instanceof PortModel from && from.getPortType().equals(PortType.MISSING_PORT) && from.getConnectedWires().isEmpty()) {
                var nodeModel = from.getNodeModel();
                if (nodeModel != null) {
                    nodeModel.removeUnusedMissingPort(from);
                }
            }
        }
    }

    public void deleteWire(WireModel wireToDelete) {
        if (wireToDelete != null && wireToDelete.isDeletable()) {
            if (wireToDelete instanceof WirePlaceHolder placeHolder) {
                removePlaceholder(placeHolder);
            } else {
                if (wireToDelete.getToPort() instanceof PortModel port && port.getNodeModel() instanceof NodeModel nodeModel) {
                    nodeModel.onDisconnection(wireToDelete.getToPort(), wireToDelete.getFromPort());
                }
                if (wireToDelete.getFromPort() instanceof PortModel port && port.getNodeModel() instanceof NodeModel nodeModel) {
                    nodeModel.onDisconnection(wireToDelete.getFromPort(), wireToDelete.getToPort());
                }

                getCurrentGraphChangeDescription().addChangedModel(wireToDelete.getToPort(), ChangeHint.GRAPH_TOPOLOGY);
                getCurrentGraphChangeDescription().addChangedModel(wireToDelete.getFromPort(), ChangeHint.GRAPH_TOPOLOGY);
                removeWire(wireToDelete);
            }
        }
    }

    /**
     * Deletes wires from the graph.
     * @param wireModels The list of wires to delete.
     */
    public void deleteWires(Collection<? extends WireModel> wireModels) {
        List.copyOf(wireModels).forEach(this::deleteWire);
    }

    /**
     * Updates a wire when one of its port changes.
     * @param wireModel The wire to update.
     * @param oldPort The old port.
     * @param newPort The new port.
     */
    public void updateWire(WireModel wireModel, PortModel oldPort, PortModel newPort) {
        if (portWireIndex != null) {
            portWireIndex.wirePortsChanged(wireModel, oldPort, newPort);
        }
        if (oldPort != null) {
            getCurrentGraphChangeDescription().addChangedModel(oldPort, ChangeHint.GRAPH_TOPOLOGY);
            if (oldPort.getPortType() == PortType.MISSING_PORT && oldPort.getConnectedPorts().isEmpty()) {
                var nodeModel = oldPort.getNodeModel();
                if (nodeModel != null) {
                    nodeModel.removeUnusedMissingPort(oldPort);
                }
            }
        }

        if (newPort != null) {
            getCurrentGraphChangeDescription().addChangedModel(newPort, ChangeHint.GRAPH_TOPOLOGY);
        }
        if (wireModel != null) {
            getCurrentGraphChangeDescription().addChangedModel(wireModel, ChangeHint.GRAPH_TOPOLOGY);
        }

        // when moving a wire to a new node, make sure it gets stored matching its new place.
        if (wireModel != null &&
                wireModel.getGraphModel() == this &&
                oldPort != null && newPort != null &&
                oldPort.getNodeModel() != newPort.getNodeModel() &&
                newPort == wireModel.getFromPort() &&
                wireModel.getFromPort().hasReorderableWires()) {
            applyReorderToGraph(wireModel.getFromPort());
        }
    }

    // endregion

    // region Group

    protected Class<? extends GroupModel> getGroupModelType() {
        return GroupModel.class;
    }

    protected Class<? extends SectionModel> getSectionModelType() {
        return SectionModel.class;
    }

    /**
     * Creates a new group.
     * @param name The name of the new group.
     * @param items An optional list of items that will be added to the group.
     * @return a new group.
     */
    public GroupModel createGroup(String name, @Nullable Collection<? extends IGroupItemModel> items) {
        var group = instantiateGroup(name);
        addGroup(group);

        if (items != null) {
            for (IGroupItemModel item : items) {
                group.insertItem(item, Integer.MAX_VALUE);
            }
        }
        return group;
    }

    /**
     * Instantiates a group model.
     */
    protected GroupModel instantiateGroup(String name) {
        var groupType = getGroupModelType();
        try {
            var group = groupType.getConstructor().newInstance();
            group.setName(name);
            group.setGraphModel(this);
            return group;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate group of type " + groupType.getName(), e);
        }
    }

    /**
     * Registers a group to the graph.
     * @param group the group.
     */
    protected void addGroup(GroupModel group) {
        // Group is not added to the graph: it will be added to a section.
        registerElement(group);
        getCurrentGraphChangeDescription().addNewModel(group);
    }

    /**
     * Creates a new {@link SectionModel} and adds it to the graph.
     * @param sectionName The name of the section.
     * @return The newly created section.
     */
    public SectionModel createSection(String sectionName) {
        var section = instantiateSection(sectionName);
        addSection(section);
        return section;
    }

    /**
     * Instantiates a {@link SectionModel}.
     */
    protected SectionModel instantiateSection(String sectionName) {
        var sectionType = getSectionModelType();
        try {
            var section = sectionType.getConstructor().newInstance();
            section.setName(sectionName);
            section.setGraphModel(this);
            return section;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate section of type " + sectionType.getName(), e);
        }
    }

    /**
     * Registers a section to the graph.
     * @param section the section.
     */
    protected void addSection(SectionModel section) {
        registerElement(section);
        sectionModels.add(section);
        getCurrentGraphChangeDescription().addNewModel(section);
    }

    /**
     * Removes a group from the graph.
     * @param section the section.
     */
    protected void removeSection(SectionModel section) {
        unregisterElement(section);
        sectionModels.remove(section);
        getCurrentGraphChangeDescription().addDeletedModel(section);
    }

    /**
     * Gets a section by name.
     * @param sectionName the name of the section.
     * @return the section.
     */
    public @Nullable SectionModel getSectionModel(String sectionName) {
        return sectionModels.stream().filter(s -> s.getName().equals(sectionName)).findAny().orElse(null);
    }

    /**
     * Returns a valid section for a given variable. Default is to return the first section {@link #DEFAULT_SECTION_NAME}.
     */
    public String getVariableSection(VariableDeclarationModelBase variable) {
        return DEFAULT_SECTION_NAME;
    }

    /**
     * Removes a group from the graph.
     * @param groupModels The group models to delete.
     */
    public void deleteGroups(Collection<? extends GroupModel> groupModels) {
        var deletedModels = new ArrayList<GraphElementModel>();
        var deletedVariables = new ArrayList<VariableDeclarationModelBase>();

        groupModels.stream().filter(GroupModel::isDeletable).forEach(group -> {
            if (group.getParentGroup() instanceof GroupModel groupModel) {
                groupModel.removeItem(group);
            }
            registerElement(group);
        });

        deleteVariableDeclarations(deletedVariables, true);
        getCurrentGraphChangeDescription().addDeletedModels(deletedModels);
    }

    private void recurseRemoveGroup(List<GraphElementModel> deletedModels, List<VariableDeclarationModelBase> deletedVariables, GroupModel groupModel) {
        removeGroup(groupModel);
        for (var item : groupModel.getItems()) {
            if (item instanceof VariableDeclarationModelBase variable)
                deletedVariables.add(variable);
            else if (item instanceof GroupModel group)
                recurseRemoveGroup(deletedModels, deletedVariables, group);
            else
                deletedModels.add((GraphElementModel)item);
        }
    }

    protected void removeGroup(GroupModel groupModel) {
        unregisterElement(groupModel);
        getCurrentGraphChangeDescription().addDeletedModel(groupModel);
    }

    // endregion

    // region Placemat & StickyNote

    public PlacematModel createPlacemat(String name, Vector2f position, Vector2f size) {
        var model = new PlacematModel();
        model.setGraphModel(this);
        model.setPosition(position);
        model.setSize(size);
        model.setName(name);
        placematModels.add(model);
        registerElement(model);
        getCurrentGraphChangeDescription().addNewModel(model);
        return model;
    }

    public StickyNoteModel createStickyNote(Vector2f position) {
        var model = new StickyNoteModel();
        model.setGraphModel(this);
        model.setPosition(position);
        stickyNoteModels.add(model);
        registerElement(model);
        getCurrentGraphChangeDescription().addNewModel(model);
        return model;
    }

    public void deletePlacemats(Collection<? extends PlacematModel> placemats) {
        var toDelete = placemats.stream().filter(PlacematModel::isDeletable).toList();
        if (!toDelete.isEmpty()) {
            removeElements(toDelete);
        }
    }

    public void deleteStickyNotes(Collection<? extends StickyNoteModel> stickyNotes) {
        var toDelete = stickyNotes.stream().filter(StickyNoteModel::isDeletable).toList();
        if (!toDelete.isEmpty()) {
            removeElements(toDelete);
        }
    }

    private void removeStickyNote(StickyNoteModel model) {
        stickyNoteModels.remove(model);
        unregisterElement(model);
        getCurrentGraphChangeDescription().addDeletedModel(model);
    }

    private void removePlacemat(PlacematModel model) {
        placematModels.remove(model);
        unregisterElement(model);
        getCurrentGraphChangeDescription().addDeletedModel(model);
    }

    // endregion

    // region Variable Declaration

    public Class<? extends VariableDeclarationModel> getVariableDeclarationModelType() {
        return VariableDeclarationModel.class;
    }

    /**
     * Indicates whether a {@link VariableDeclarationModel} requires initialization.
     * @param decl The variable declaration model to query.
     * @return True if the variable declaration model requires initialization, false otherwise.
     */
    public boolean variableDeclarationRequiresInitialization(VariableDeclarationModelBase decl) {
        return decl.requiresInitialization();
    }

    public VariableDeclarationModel createGraphVariableDeclaration(TypeHandle variableDataType,
                                                                   String variableName,
                                                                   ModifierFlags modifierFlags,
                                                                   VariableScope scope,
                                                                   @Nullable GroupModel group,
                                                                   int indexInGroup,
                                                                   @Nullable Constant initializationModel,
                                                                   @Nullable UUID uid,
                                                                   @Nullable SpawnFlags spawnFlags) {
        modifierFlags = sanitizeSubgraphVariableModifiers(modifierFlags);
        if (isContainerGraph() && (modifierFlags == ModifierFlags.READ || modifierFlags == ModifierFlags.WRITE)) {
            LDLib2.LOGGER.warn("Cannot create an input or an output variable declaration in a container graph.");
            return null;
        }

        if (!allowExposedVariableCreation() && scope == VariableScope.EXPOSED) {
            LDLib2.LOGGER.warn("This graph doesn't allow the creation of a variable declaration with an exposed scope. A variable declaration with a local scope is created instead.");
            scope = VariableScope.LOCAL;
        }

        return createGraphVariableDeclaration(getVariableDeclarationModelType(), variableDataType, variableName,
                modifierFlags, scope, group, indexInGroup, initializationModel, uid, (variableDeclaration, initModel) -> {
                    if (variableDeclaration != null) {
                        variableDeclaration.setVariableFlags(VariableFlags.NONE);
                        if (initModel != null) variableDeclaration.setInitializationModel(initModel);
                    }
                }, spawnFlags);
    }

    /**
     * Creates a new variable declaration in the graph.
     * @param variableTypeToCreate The type of variable declaration to create.
     * @param variableDataType The type of data the new variable declaration to create represents.
     * @param variableName The name of the new variable declaration to create.
     * @param modifierFlags The modifier flags of the new variable declaration to create.
     * @param scope The scope of the variable.
     * @param group The group in which the variable is added. If null, it will go to the root group.
     * @param indexInGroup The index of the variable in the group. For {@code indexInGroup=0}, The item will be added at the beginning. For {@code indexInGroup=Items.size()}, items will be added at the end.
     * @param initializationModel The initialization model of the new variable declaration to create. Can be {@code null}..
     * @param uid The unique identifier (UUID) to assign to the newly created item.
     * @param initializationCallback An initialization method to be called right after the variable declaration is created.
     * @param spawnFlags The flags specifying how the variable declaration is to be spawned.
     * @return The newly created variable declaration.
     */
    public VariableDeclarationModel createGraphVariableDeclaration(Class<? extends VariableDeclarationModel> variableTypeToCreate,
                                                                   TypeHandle variableDataType,
                                                                   String variableName,
                                                                   ModifierFlags modifierFlags,
                                                                   VariableScope scope,
                                                                   @Nullable GroupModel group,
                                                                   int indexInGroup,
                                                                   @Nullable Constant initializationModel,
                                                                   @Nullable UUID uid,
                                                                   @Nullable BiConsumer<VariableDeclarationModelBase, Constant> initializationCallback,
                                                                   @Nullable SpawnFlags spawnFlags) {
        modifierFlags = sanitizeSubgraphVariableModifiers(modifierFlags);
        if (isContainerGraph() && (modifierFlags == ModifierFlags.READ || modifierFlags == ModifierFlags.WRITE)) {
            LDLib2.LOGGER.warn("Cannot create an input or an output variable declaration in a container graph.");
            return null;
        }


        var variableDeclaration = instantiateVariableDeclaration(variableTypeToCreate, variableDataType,
                variableName, modifierFlags, scope, initializationModel, uid, initializationCallback);

        if (variableDeclaration == null)
            return null;

        if (spawnFlags == null) spawnFlags = SpawnFlags.NONE;
        if (!spawnFlags.isOrphan())
            addVariableDeclaration(variableDeclaration);

        if (group != null) {
            group.insertItem(variableDeclaration, indexInGroup);
        } else {
            var section = variableDeclaration.getGraphModel().getSectionModel(variableDeclaration.getGraphModel().getVariableSection(variableDeclaration));
            if (section != null) {
                section.insertItem(variableDeclaration, indexInGroup);
            }
        }

        // TODO does it a bug? uid is not set here.
        var data = new PlaceholderData();
        data.setGroupName(variableDeclaration.getParentGroup().getName());
        placeholderData.put(uid, data);

        if (modifierFlags != ModifierFlags.NONE) {
            redefineSubgraphNodeModels();
        }

        return variableDeclaration;
    }

    protected VariableDeclarationModel instantiateVariableDeclaration(Class<? extends VariableDeclarationModel> variableTypeToCreate,
                                                                      TypeHandle variableDataType,
                                                                      String variableName,
                                                                      ModifierFlags modifierFlags,
                                                                      VariableScope scope,
                                                                      @Nullable Constant initializationModel,
                                                                      @Nullable UUID uid,
                                                                      @Nullable BiConsumer<VariableDeclarationModelBase, Constant> initializationCallback) {
        try {
            var variableDeclaration = variableTypeToCreate.getConstructor().newInstance();
            if (uid != null) {
                variableDeclaration.setUid(uid);
            }
            variableDeclaration.setGraphModel(this);
            variableDeclaration.setDataTypeHandle(variableDataType);
            if (initializationModel != null) {
                variableDeclaration.setInitializationModel(initializationModel);
            }
            variableDeclaration.setName(generateGraphVariableDeclarationUniqueName(variableName));
            variableDeclaration.setScope(scope);
            variableDeclaration.setModifiers(modifierFlags);

            if (initializationCallback != null) {
                initializationCallback.accept(variableDeclaration, variableDeclaration.getInitializationModel());
            }

            return variableDeclaration;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate variable declaration of type " + variableTypeToCreate.getName(), e);
        }
    }

    /**
     * Generates a unique name for a variable declaration in the graph.
     * @param originalName The name of the variable declaration.
     * @return The unique name for the variable declaration.
     */
    protected String generateGraphVariableDeclarationUniqueName(String originalName) {
        var index = 0;
        var baseName = originalName;
        while (existingVariableNames.contains(originalName)) {
            originalName = baseName + "." + index++;
        }
        return originalName;
    }

    /**
     * Adds a variable declaration to the graph.
     */
    protected void addVariableDeclaration(VariableDeclarationModelBase variableDeclaration) {
        registerElement(variableDeclaration);
        // todo meta
//        AddMetaData(variableDeclarationModel, m_GraphVariableModels.Count);
        graphVariableModels.add(variableDeclaration);
        existingVariableNames.add(variableDeclaration.getName());
        getCurrentGraphChangeDescription().addNewModel(variableDeclaration);
    }

    /**
     * Deletes the given variable declaration model, with the option of also deleting the corresponding variable models.
     * @param variableModel The variable declaration model to delete.
     * @param deleteUsages Whether to delete the corresponding variable models.
     */
    public void deleteVariableDeclaration(VariableDeclarationModelBase variableModel, boolean deleteUsages) {
        if (!variableModel.isDeletable()) return;

        if (variableModel instanceof VariableDeclarationPlaceholder placeholderModel) {
            removePlaceholder(placeholderModel);
        }

        removeVariableDeclaration(variableModel);

        if (deleteUsages) {
            var nodesToDelete = findReferencesInGraph(AbstractNodeModel.class, variableModel);
            deleteNodes(nodesToDelete, true, true);
        }
    }

    /**
     * Deletes the given variable declaration models, with the option of also deleting the corresponding variable models.
     */
    public void deleteVariableDeclarations(Collection<? extends VariableDeclarationModelBase> variableModels, boolean deleteUsages) {
        for (var variableModel : variableModels) {
            deleteVariableDeclaration(variableModel, deleteUsages);
        }
    }

    protected GroupModelBase removeVariableDeclaration(VariableDeclarationModelBase variableDeclarationModel) {
        if (variableDeclarationModel == null)
            return null;

        unregisterElement(variableDeclarationModel);

        var indexToRemove = -1;
        for (var i = 0; i < graphVariableModels.size(); i++) {
            var variable = graphVariableModels.get(i);
            if (variable == null)
                continue;
            if (variableDeclarationModel.getUid().equals(variable.getUid())) {
                indexToRemove = i;
                break;
            }
        }

        if (indexToRemove != -1) {
            // todo meta
//            RemoveFromMetadata(indexToRemove, ManagedMissingTypeModelCategory.VariableDeclaration);
            graphVariableModels.remove(indexToRemove);
            graphVariableModels.add(indexToRemove, null);
            getCurrentGraphChangeDescription().addDeletedModel(variableDeclarationModel);
        }

        existingVariableNames.remove(variableDeclarationModel.getName());

        var parent = variableDeclarationModel.getParentGroup();
        if (parent instanceof GroupModel group) {
            group.removeItem(variableDeclarationModel);
        }

        // exposed-variable removal must update outer subgraph node ports
        var mods = variableDeclarationModel.getModifiers();
        if (mods != null && mods != ModifierFlags.NONE) {
            redefineSubgraphNodeModels();
        }

        return parent;
    }

    // endregion

    // region Placeholders

    protected void removePlaceholder(IPlaceHolder placeholder) {
        var model = getElementsByUID().get(placeholder.getUid());
        if (model != null) {
            unregisterElement(model);
            getCurrentGraphChangeDescription().addDeletedModel(model);
        }

        // todo reference and meta data
        // Clear the serialized data related to the null object the user wants to remove.
//        SerializationUtility.ClearManagedReferenceWithMissingType(GraphObject, placeholder.ReferenceId);

//        var metadata = m_GraphElementMetaData.FirstOrDefault(m => m.Guid == placeholder.Guid);
//
//        // It is not possible to distinguish the index of objects with a missing type in the serialization. Hence, we keep a flag and remove the corresponding null object on the next graph reload.
//        if (metadata != null)
//            metadata.ToRemove = true;

        // Remove the placeholder
        placeholders.remove(placeholder);
    }

    // endregion

    // region Portal Declaration

    /**
     * Finds all node models that refer to a given declaration model.
     */
    public <T> List<T> findReferencesInGraph(Class<T> type, DeclarationModel declarationModel) {
        if (declarationModel == null) return Collections.emptyList();
        var result = new ArrayList<T>();
        for (var nodeModel : getNodeModels()) {
            if (nodeModel instanceof IHasDeclarationModel hasDeclarationModel
                    && hasDeclarationModel.getDeclarationModel() != null
                    && hasDeclarationModel.getDeclarationModel().getUid().equals(declarationModel.getUid())
                    && type.isInstance(hasDeclarationModel)) {
                result.add((T) nodeModel);
            }
        }
        return result;
    }

    /**
     * Finds all entry portals that refer to a given declaration model.
     * @param declarationModel The declaration model to look for.
     * @return A list of entry portals that refer to the given declaration model.
     */
    public List<WirePortalModel> getEntryPortals(DeclarationModel declarationModel) {
        var result = new ArrayList<WirePortalModel>();
        var allRefs = findReferencesInGraph(WirePortalModel.class, declarationModel);
        for (var ref : allRefs) {
            if (ref instanceof ISingleInputPortNodeModel) {
                result.add(ref);
            }
        }
        return result;
    }

    /**
     * Finds all exit portals that refer to a given declaration model.
     */
    public List<WirePortalModel> getExitPortals(DeclarationModel declarationModel) {
        var result = new ArrayList<WirePortalModel>();
        var allRefs = findReferencesInGraph(WirePortalModel.class, declarationModel);
        for (var ref : allRefs) {
            if (ref instanceof ISingleOutputPortNodeModel) {
                result.add(ref);
            }
        }
        return result;
    }

    protected void addPortal(DeclarationModel declarationModel) {
        if (!allowPortalCreation()) {
            throw new IllegalArgumentException("Wire portal creation is disabled.");
        }

        registerElement(declarationModel);
        // todo meta data
//        AddMetaData(declarationModel, m_GraphPortalModels.Count);
        portalModels.add(declarationModel);
        getCurrentGraphChangeDescription().addNewModel(declarationModel);
    }

    protected void removePortal(DeclarationModel declarationModel) {
        if (declarationModel == null) return;
        unregisterElement(declarationModel);
        var index = -1;
        for (int i = 0; i < portalModels.size(); i++) {
            var portal = portalModels.get(i);
            if (portal == null) continue;
            if (portal.getUid().equals(declarationModel.getUid())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            portalModels.remove(index);
            portalModels.add(index, null);
            getCurrentGraphChangeDescription().addDeletedModel(declarationModel);
        }
    }

    /**
     * Creates a pair of portals from a wire.
     * @param wireModel The wire to transform.
     * @param entryPortalPosition The desired position of the entry portal.
     * @param exitPortalPosition The desired position of the exit portal.
     * @param portalHeight The desired height of the portals.
     * @param existingPortalEntries The existing portal entries.
     * @param existingPortalExits The existing portal exits.
     */
    public void createPortalsFromWire(WireModel wireModel,
                                      Vector2f entryPortalPosition, Vector2f exitPortalPosition,
                                      int portalHeight,
                                      Map<PortModel, WirePortalModel> existingPortalEntries,
                                      Map<PortModel, List<WirePortalModel>> existingPortalExits) {
        if (!allowPortalCreation()) throw new IllegalArgumentException("Wire portal creation is disabled.");
        var inputPortModel = wireModel.getToPort();
        var outputPortModel = wireModel.getFromPort();

        // Only a single portal per output port. Don't recreate if we already created one.
        var portalEntry = existingPortalEntries.get(outputPortModel);

        if (outputPortModel != null && portalEntry == null) {
            portalEntry = createEntryPortalFromPort(outputPortModel, entryPortalPosition, portalHeight, null, 0);
            wireModel.setPort(WireSide.TO, (portalEntry instanceof ISingleInputPortNodeModel in) ?
                    in.getInputPort() : null);
            existingPortalEntries.put(outputPortModel, portalEntry);
            getCurrentGraphChangeDescription().addChangedModel(wireModel, ChangeHint.LAYOUT);
        } else {
            deleteWires(Collections.singletonList(wireModel));
        }

        // We can have multiple portals on input ports however
        var portalExit = createExitPortalToPort(inputPortModel, exitPortalPosition, portalHeight, portalEntry.getDeclarationModel(), 0);
        existingPortalExits.computeIfAbsent(wireModel.getToPort(), k -> new ArrayList<>()).add(portalExit);

        createWire(inputPortModel, (portalExit instanceof ISingleOutputPortNodeModel out) ? out.getOutputPort() : null, null);
    }


    /**
     * Creates an exit portal matching a port.
     * @param outputPortModel The output port model to which the portal will be connected.
     * @param position The desired position of the entry portal.
     * @param height The desired height of the entry portal.
     * @param declarationModel The declaration of the portal. If null, a new one will be created.
     * @param offset The offset to apply to the portal.
     * @return The created entry portal.
     */
    public WirePortalModel createEntryPortalFromPort(PortModel outputPortModel,
                                                     Vector2f position,
                                                     int height,
                                                     @Nullable DeclarationModel declarationModel,
                                                     float offset) {
        if (!allowPortalCreation()) throw new IllegalArgumentException("Wire portal creation is disabled.");
        if (!(outputPortModel.getNodeModel() instanceof InputOutputPortsNodeModel nodeModel)) return null;

        String portalName ;
        if (nodeModel instanceof ConstantNodeModel constantNodeModel) {
            portalName = TypeHandleHelpers.identificationOf(constantNodeModel.getType());
        } else {
            portalName = nodeModel.getName();
            var portName = outputPortModel.getName();
            if (portName != null) {
                portalName = portalName + " - " + portName;
            }
        }

        var portalEntry = createWirePortalNode(
                getWirePortalEntryType(),
                declarationModel == null ? createGraphPortalDeclaration(portalName, null, null) : declarationModel,
                outputPortModel.getDataTypeHandle(),
                position,
                null, null, null, null);

        // y offset based on port order. hurgh.
        var idx = nodeModel.getOutputsByDisplayOrder().indexOf(outputPortModel);
        portalEntry.setPosition(portalEntry.getPosition().add(0, (idx * height + offset), new Vector2f()));
        return portalEntry;
    }

    /**
     * Creates an exit portal matching a port.
     */
    public WirePortalModel createExitPortalToPort(PortModel inputPortModel,
                                                  Vector2f position,
                                                  int height,
                                                  DeclarationModel declarationModel,
                                                  float offset) {
        if (!allowPortalCreation()) throw new IllegalArgumentException("Wire portal creation is disabled.");

        var portalExit = createWirePortalNode(
                getWirePortalExitType(),
                declarationModel,
                inputPortModel.getDataTypeHandle(),
                position,
                null, null, null, null);

        portalExit.setPosition(position);
        if (inputPortModel.getNodeModel() instanceof InputOutputPortsNodeModel nodeModel){
            // y offset based on port order. hurgh.
            var idx = nodeModel.getInputsByDisplayOrder().indexOf(inputPortModel);
            portalExit.setPosition(portalExit.getPosition().add(0, (idx * height + offset), new Vector2f()));
        }

        return portalExit;
    }

    public WirePortalModel createWirePortalNode(Class<?> portalType,
                                                 DeclarationModel declarationModel,
                                                 TypeHandle portDataTypeHandle,
                                                 Vector2f position,
                                                 @Nullable String name,
                                                 @Nullable UUID uid,
                                                 @Nullable Consumer<AbstractNodeModel> initializationCallback,
                                                 @Nullable SpawnFlags spawnFlags) {
        if (name == null) name = "";
        if (spawnFlags == null) spawnFlags = SpawnFlags.DEFAULT;

        return (WirePortalModel) createNode(portalType, name, position, uid, n -> {
            if (n instanceof WirePortalModel wirePortalModel) {
                wirePortalModel.setPortDataTypeHandle(portDataTypeHandle);
                wirePortalModel.setDeclarationModel(declarationModel);
            }
            if (initializationCallback != null) initializationCallback.accept(n);
        }, spawnFlags);
    }

    /**
     * Creates a new declaration model representing a portal and optionally add it to the graph.
     */
    public DeclarationModel createGraphPortalDeclaration(String portalName,
                                                         @Nullable UUID uid,
                                                         @Nullable SpawnFlags spawnFlags) {
        if (!allowPortalCreation()) throw new IllegalArgumentException("Wire portal creation is disabled.");
        if (spawnFlags == null) spawnFlags = SpawnFlags.NONE;

        var decl = instantiatePortalDeclaration(portalName, uid);

        if (!spawnFlags.isOrphan()) {
            addPortal(decl);
        }

        return decl;
    }

    /**
     * Instantiates a new portal model.
     */
    protected DeclarationModel instantiatePortalDeclaration(String name, @Nullable UUID uid) {
        if (!allowPortalCreation()) throw new IllegalArgumentException("Wire portal creation is disabled.");

        var portalModel = new DeclarationModel();
        portalModel.setName(name);
        if (uid != null) portalModel.setUid(uid);
        portalModel.setGraphModel(this);
        return portalModel;
    }

    // endregion

    // region subgraph extraction

    /** Tracks a wire crossing the selection boundary. */
    private record CrossingWire(WireModel wire, boolean fromSelected) {}

    /**
     * Extracts a heterogeneous selection (nodes, placemats, sticky notes — wires are ignored as
     * they're implicit in the node selection) into a fresh local subgraph and inserts a
     * {@link SubgraphNodeModel} at the selection's centroid that references it.
     *
     * <p>Selection-handling rules:</p>
     * <ul>
     *   <li><b>{@link WireModel}</b> — filtered out. Internal wires (both endpoints in the
     *       selected nodes) are copied automatically by {@link #copyElements}; crossing wires
     *       are reconnected via auto-generated variables (see below).</li>
     *   <li><b>{@link PlacematModel}</b> — accepted only if all its currently contained nodes
     *       are also in the selection; otherwise rejected (we'd leave dangling nodes outside).
     *       The placemat itself is moved into the subgraph.</li>
     *   <li><b>{@link StickyNoteModel}</b> — moved into the subgraph as-is.</li>
     *   <li><b>{@link SubgraphNodeModel}</b> (LOCAL) — its referenced local subgraph is
     *       transferred from this graph's {@code localSubGraphs} to the newly created one's
     *       <em>before paste</em>, so the pasted SubgraphNodeModel can resolve to it.</li>
     *   <li><b>{@link SubgraphNodeModel}</b> (EXTERNAL) — copy/paste handles it; only the
     *       {@code IResourcePath} reference travels, no graph data is moved.</li>
     * </ul>
     *
     * <p>Crossing wires are preserved by minting a variable inside the new subgraph for each
     * (READ for inbound value, WRITE for outbound), wiring a {@code VariableNodeModel} to the
     * pasted internal port, and the outer SubgraphNodeModel's auto-port to the original external
     * port.</p>
     *
     * @return the newly created outer subgraph node, or {@code null} if extraction failed.
     */
    @Nullable
    public SubgraphNodeModel extractSelectionToLocalSubgraph(List<? extends GraphElementModel> selection,
                                                             HolderLookup.Provider provider) {
        if (selection == null || selection.isEmpty()) return null;
        if (!allowSubgraphCreation()) {
            LDLib2.LOGGER.warn("Subgraph creation is disabled on this graph.");
            return null;
        }

        // Partition the heterogeneous selection. Wires are filtered out — internal wires get
        // copied implicitly by copyElements, crossing wires get reconnected via variables.
        var selectedNodes = new ArrayList<AbstractNodeModel>();
        var selectedPlacemats = new ArrayList<PlacematModel>();
        var selectedStickyNotes = new ArrayList<StickyNoteModel>();
        for (var element : selection) {
            if (element instanceof WireModel) {
                // ignored
            } else if (element instanceof AbstractNodeModel n) {
                selectedNodes.add(n);
            } else if (element instanceof PlacematModel pm) {
                selectedPlacemats.add(pm);
            } else if (element instanceof StickyNoteModel sn) {
                selectedStickyNotes.add(sn);
            } else {
                LDLib2.LOGGER.warn("Ignoring unsupported selection element type: {}",
                        element.getClass().getName());
            }
        }

        if (selectedNodes.isEmpty() && selectedPlacemats.isEmpty() && selectedStickyNotes.isEmpty()) {
            LDLib2.LOGGER.warn("Cannot extract: selection contains no movable elements.");
            return null;
        }

        // Copiability check on the non-wire elements
        for (var n : selectedNodes) {
            if (!n.isCopiable()) {
                LDLib2.LOGGER.warn("Cannot extract: selection contains a non-copiable node {}.", n.getUid());
                return null;
            }
        }

        var selectedNodeUids = selectedNodes.stream()
                .map(AbstractNodeModel::getUid)
                .collect(Collectors.toSet());

        // Placemats: every node currently inside must also be selected. We do a position-only
        // check (matches the fallback in PlacematModel.getContainedNodes when size lookup absent)
        // — selection-from-rectangle UI typically already selects the contained nodes.
        for (var pm : selectedPlacemats) {
            var contained = pm.getContainedNodes(null);
            for (var n : contained) {
                if (!selectedNodeUids.contains(n.getUid())) {
                    LDLib2.LOGGER.warn(
                            "Cannot extract: placemat {} contains a non-selected node {}; "
                                    + "select the node or remove the placemat from the selection.",
                            pm.getUid(), n.getUid());
                    return null;
                }
            }
        }

        // Identify any LOCAL SubgraphNodeModels in the selection — their referenced local
        // subgraph must be transplanted from this graph's localSubGraphs into the new subgraph's
        // localSubGraphs so the pasted SubgraphNodeModel can resolve it (resolution is by uid).
        var localSubsToTransplant = new ArrayList<GraphModel>();
        for (var n : selectedNodes) {
            if (n instanceof SubgraphNodeModel sub
                    && sub.getKind() == SubgraphNodeModel.Kind.LOCAL) {
                var target = sub.getSubgraphModel();
                if (target != null && this.localSubGraphs != null
                        && this.localSubGraphs.contains(target)) {
                    localSubsToTransplant.add(target);
                }
            }
        }

        // Centroid for placement of the new outer SubgraphNodeModel — use only node positions
        // for stability (placemats/sticky notes may be much larger).
        var centroid = new Vector2f();
        var centroidSrc = selectedNodes.isEmpty() ? (List<? extends IMovable>) selectedPlacemats : selectedNodes;
        if (centroidSrc.isEmpty()) centroidSrc = selectedStickyNotes;
        for (var m : centroidSrc) centroid.add(m.getPosition());
        if (!centroidSrc.isEmpty()) centroid.div(centroidSrc.size());

        // Crossing wires — only consider wires touching selected nodes (placemats/sticky notes
        // have no ports). Wires explicitly in the selection are not relevant for boundary logic.
        var crossing = new ArrayList<CrossingWire>();
        for (var wire : wireModels) {
            if (wire == null) continue;
            var fromPort = wire.getFromPort();
            var toPort = wire.getToPort();
            if (fromPort == null || toPort == null) continue;
            var fromNode = fromPort.getNodeModel();
            var toNode = toPort.getNodeModel();
            if (fromNode == null || toNode == null) continue;
            boolean fromSel = selectedNodeUids.contains(fromNode.getUid());
            boolean toSel = selectedNodeUids.contains(toNode.getUid());
            if (fromSel == toSel) continue;
            crossing.add(new CrossingWire(wire, fromSel));
        }

        // Build the list passed to copyElements: nodes + placemats + sticky notes
        var elementsToCopy = new ArrayList<GraphElementModel>(selectedNodes.size()
                + selectedPlacemats.size() + selectedStickyNotes.size());
        elementsToCopy.addAll(selectedNodes);
        elementsToCopy.addAll(selectedPlacemats);
        elementsToCopy.addAll(selectedStickyNotes);

        // New empty subgraph — created BEFORE copy so we can transplant local-subgraph references
        // out of `this.localSubGraphs` ahead of time. With them gone, copyElements' local-subgraph
        // deep-clone logic won't see them (getSubgraphModel returns null) and won't produce a
        // redundant clone — paste leaves the pasted SubgraphNodeModel's localGraphId untouched
        // and it resolves correctly inside newSub.
        var sub = createLocalSubgraphInstance();
        if (sub == null) {
            LDLib2.LOGGER.warn("Graph type does not support inline subgraphs: {}", this.getClass().getName());
            return null;
        }
        addLocalSubgraph(sub);

        // Transplant any selected LOCAL subgraphs from this.localSubGraphs into sub.localSubGraphs.
        for (var moved : localSubsToTransplant) {
            this.localSubGraphs.remove(moved);
            sub.addLocalSubgraph(moved);
        }

        // Snapshot the selection for copy AFTER transplant — selected SubgraphNodeModels no
        // longer resolve their inner graph via outer, so copyElements skips them in its
        // local-subgraph-clone pass (the transplanted graph travels via the parent-pointer
        // reattachment instead).
        var copyData = copyElements(elementsToCopy, provider);

        // Variables inside the subgraph that mirror each crossing wire
        var crossingVars = new HashMap<CrossingWire, VariableDeclarationModel>();
        int inCounter = 0, outCounter = 0;
        for (var c : crossing) {
            ModifierFlags mod;
            String varName;
            TypeHandle type = c.fromSelected
                    ? c.wire.getFromPort().getDataTypeHandle()
                    : c.wire.getToPort().getDataTypeHandle();
            if (c.fromSelected) {
                mod = ModifierFlags.WRITE;
                varName = "out" + (++outCounter);
            } else {
                mod = ModifierFlags.READ;
                varName = "in" + (++inCounter);
            }
            var vdm = sub.createGraphVariableDeclaration(type, varName, mod,
                    VariableScope.LOCAL, null, Integer.MAX_VALUE, null, null, null);
            if (vdm != null) crossingVars.put(c, vdm);
        }

        // Paste copy into subgraph; offset positions so the cluster sits around (0,0) inside
        var pasted = sub.pasteElementsWithMap(copyData, new Vector2f(-centroid.x, -centroid.y));
        var oldToNew = pasted.oldToNewNodeMap();

        // Wire each variable's VariableNodeModel to its pasted internal port
        for (var c : crossing) {
            var vdm = crossingVars.get(c);
            if (vdm == null) continue;
            var internalOldNode = c.fromSelected
                    ? c.wire.getFromPort().getNodeModel()
                    : c.wire.getToPort().getNodeModel();
            var internalPortName = c.fromSelected
                    ? c.wire.getFromPort().getUniqueName()
                    : c.wire.getToPort().getUniqueName();
            var pastedNode = oldToNew.get(internalOldNode.getUid());
            if (pastedNode == null) continue;
            var pastedPort = findPortByUniqueName(pastedNode, internalPortName);
            if (pastedPort == null) continue;

            float dx = c.fromSelected ? 80f : -80f;
            var pos = new Vector2f(pastedNode.getPosition().x + dx, pastedNode.getPosition().y);
            var varNode = sub.createVariableNode(vdm, pos, null, null);
            PortModel varPort = c.fromSelected ? varNode.getInputPort() : varNode.getOutputPort();
            if (varPort == null) continue;
            if (c.fromSelected) {
                sub.createWire(varPort, pastedPort);
            } else {
                sub.createWire(pastedPort, varPort);
            }
        }

        // Outer SubgraphNodeModel — defineNode (via onCreateNode) builds ports from the variables
        var subNode = createNodeWithType(
                SubgraphNodeModel.class, "Subgraph", new Vector2f(centroid),
                null, n -> n.setLocalSubgraph(sub), SpawnFlags.DEFAULT);

        // Outer wires from external ports → the auto-generated SubgraphNodeModel ports
        for (var c : crossing) {
            var vdm = crossingVars.get(c);
            if (vdm == null) continue;
            var portId = vdm.getUid().toString();
            PortModel subNodePort = c.fromSelected
                    ? subNode.getOutputsById().get(portId)
                    : subNode.getInputsById().get(portId);
            if (subNodePort == null) continue;
            var externalPort = c.fromSelected ? c.wire.getToPort() : c.wire.getFromPort();
            if (externalPort == null) continue;
            if (c.fromSelected) {
                createWire(externalPort, subNodePort);
            } else {
                createWire(subNodePort, externalPort);
            }
        }

        // Remove the originals from the outer graph. deleteNodes cascades wires.
        deleteNodes(selectedNodes, true, true);
        if (!selectedPlacemats.isEmpty()) deletePlacemats(selectedPlacemats);
        if (!selectedStickyNotes.isEmpty()) deleteStickyNotes(selectedStickyNotes);

        return subNode;
    }

    // endregion

    // region subgraph

    /**
     * Recursively propagates the editor reference resolver to all nested local subgraphs so
     * external subgraph nodes nested inside locals can still resolve their inner graphs.
     */
    public void setReferenceResolver(@Nullable IGraphReferenceResolver resolver) {
        this.referenceResolver = resolver;
        if (localSubGraphs != null) {
            for (var sub : localSubGraphs) {
                if (sub != null) sub.setReferenceResolver(resolver);
            }
        }
    }

    /**
     * Adds a freshly-created local subgraph to this graph and wires its parent pointer.
     */
    public void addLocalSubgraph(GraphModel subgraphModel) {
        if (localSubGraphs == null) localSubGraphs = new ArrayList<>();
        if (!localSubGraphs.contains(subgraphModel)) {
            localSubGraphs.add(subgraphModel);
        }
        subgraphModel.parentGraph = this;
        subgraphModel.setReferenceResolver(this.referenceResolver);
    }

    public void removeLocalSubgraph(GraphModel subgraphModel) {
        if (localSubGraphs != null) {
            localSubGraphs.remove(subgraphModel);
            if (subgraphModel != null && subgraphModel.parentGraph == this) {
                subgraphModel.parentGraph = null;
            }
        }
    }

    /**
     * Looks up a local subgraph by its uid. Local subgraphs are identified by the GraphModel's own uid.
     */
    @Nullable
    public GraphModel findLocalSubgraphByUid(UUID uid) {
        if (localSubGraphs == null || uid == null) return null;
        for (var sub : localSubGraphs) {
            if (sub != null && uid.equals(sub.getUid())) return sub;
        }
        return null;
    }

    /**
     * Factory for a new empty same-typed local subgraph. The abstract base cannot instantiate
     * itself; concrete subclasses (e.g. {@link CustomGraphModelImpl}) override this. Returns null
     * if the concrete type can't be instantiated — the caller must handle.
     */
    @Nullable
    public GraphModel createLocalSubgraphInstance() {
        return null;
    }

    /**
     * Factory for a new empty local subgraph of a (possibly different) graph type. When
     * {@code graphType} is {@code null} or equal to this graph's own type, behaves like
     * {@link #createLocalSubgraphInstance()}. Cross-type instances are gated by
     * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph#acceptsSubgraphGraph}.
     * Returns {@code null} if the type can't be instantiated or isn't accepted.
     */
    @Nullable
    public GraphModel createLocalSubgraphInstance(@Nullable Class<? extends Graph> graphType) {
        return createLocalSubgraphInstance();
    }

    /**
     * If this GraphModel is a subgraph, any subgraph nodes that reference it in the parent graph must redefine its ports whenever an input or output variable declaration is added.
     */
    public void redefineSubgraphNodeModels() {
        if (parentGraph == null) return;
        for (var node : parentGraph.nodeModels) {
            if (node instanceof SubgraphNodeModel sub && sub.getSubgraphModel() == this) {
                sub.defineNode();
                parentGraph.getCurrentGraphChangeDescription()
                        .addChangedModel(sub, ChangeHint.GRAPH_TOPOLOGY);
            }
        }
    }

    /**
     * Redefines all subgraph nodes in any open graph that reference the given external resource path.
     * Called when an external asset graph is saved.
     */
    public void redefineSubgraphNodeModelsByPath(IResourcePath path) {
        if (path == null) return;
        for (var node : nodeModels) {
            if (node instanceof SubgraphNodeModel sub
                    && sub.getKind() == SubgraphNodeModel.Kind.EXTERNAL
                    && path.equals(sub.getExternalPath())) {
                sub.invalidateResolvedSubgraph();
                sub.defineNode();
                getCurrentGraphChangeDescription().addChangedModel(sub, ChangeHint.GRAPH_TOPOLOGY);
            }
        }
        if (localSubGraphs != null) {
            for (var sub : localSubGraphs) {
                if (sub != null) sub.redefineSubgraphNodeModelsByPath(path);
            }
        }
    }

    /**
     * Calls update recursively on all subgraph nodes in the graph.
     */
    public void updateSubGraphs() {
        for (var node : nodeModels) {
            if (node instanceof SubgraphNodeModel sub) {
                sub.defineNode();
            }
        }
        if (localSubGraphs != null) {
            for (var sub : localSubGraphs) {
                if (sub != null) sub.updateSubGraphs();
            }
        }
    }


    // endregion

    // region Serialization

    /**
     * Gets a type discriminator string for the given node model.
     */
    protected String getNodeDiscriminator(AbstractNodeModel node) {
        // Order matters: CustomContextNodeModelImpl is a NodeModel and also an ICustomNodeModel —
        // check the context branch before the generic custom branch.
        if (node instanceof ContextNodeModel) return "context";
        if (node instanceof CustomNodeModelImpl) return "custom";
        if (node instanceof VariableNodeModelImpl) return "variable";
        if (node instanceof ConstantNodeModelImpl) return "constant";
        if (node instanceof SubgraphNodeModel) return "subgraph";
        if (node instanceof WirePortalEntryModel) return "wire_portal_entry";
        if (node instanceof WirePortalExitModel) return "wire_portal_exit";
        throw new IllegalArgumentException("Unknown node type: " + node.getClass());
    }

    /**
     * Creates a node model instance from a discriminator string.
     */
    protected AbstractNodeModel createNodeFromDiscriminator(String type) {
        return switch (type) {
            case "custom" -> new CustomNodeModelImpl();
            case "context" -> new CustomContextNodeModelImpl();
            case "variable" -> new VariableNodeModelImpl();
            case "constant" -> new ConstantNodeModelImpl();
            case "subgraph" -> new SubgraphNodeModel();
            case "wire_portal_entry" -> new WirePortalEntryModel();
            case "wire_portal_exit" -> new WirePortalExitModel();
            default -> throw new IllegalArgumentException("Unknown node type: " + type);
        };
    }

    /**
     * Recursively serializes the items of a group (section or group) into a ListTag.
     * Each item is stored as a CompoundTag with its uid, type ("variable" or "group"),
     * and for groups, their serialized data and nested children.
     */
    private ListTag serializeGroupItems(GroupModelBase group, HolderLookup.Provider provider) {
        var itemsTag = new ListTag();
        for (var item : group.getItems()) {
            var itemTag = new CompoundTag();
            if (item instanceof VariableDeclarationModelBase variable) {
                itemTag.putString("type", "variable");
                itemTag.putUUID("uid", variable.getUid());
            } else if (item instanceof GroupModel groupItem) {
                itemTag.putString("type", "group");
                itemTag.putUUID("uid", groupItem.getUid());
                // Serialize group's own data (name, etc.)
                var groupData = groupItem.serializeNBT(provider);
                itemTag.put("data", groupData);
                // Recursively serialize children
                itemTag.put("items", serializeGroupItems(groupItem, provider));
            }
            itemsTag.add(itemTag);
        }
        return itemsTag;
    }

    /**
     * Recursively deserializes group items and rebuilds the parent-child hierarchy.
     */
    private void deserializeGroupItems(GroupModel parent, ListTag itemsTag, HolderLookup.Provider provider) {
        for (int i = 0; i < itemsTag.size(); i++) {
            var itemTag = itemsTag.getCompound(i);
            var type = itemTag.getString("type");
            var uid = itemTag.getUUID("uid");
            switch (type) {
                case "variable" -> {
                    var model = getModel(uid);
                    if (model instanceof IGroupItemModel variable) {
                        parent.insertItem(variable, parent.getItems().size());
                    }
                }
                case "group" -> {
                    var group = new GroupModel();
                    group.setGraphModel(this);
                    group.deserializeNBT(provider, itemTag.getCompound("data"));
                    registerElement(group);
                    parent.insertItem(group, parent.getItems().size());
                    // Recursively rebuild children
                    if (itemTag.contains("items")) {
                        deserializeGroupItems(group, itemTag.getList("items", Tag.TAG_COMPOUND), provider);
                    }
                }
            }
        }
    }

    @Override
    public Tag serializeAdditionalNBT(HolderLookup.@NotNull Provider provider) {
        var tag = new CompoundTag();

        // 1. Variables
        var variablesTag = new ListTag();
        for (var variable : graphVariableModels) {
            if (variable instanceof VariableDeclarationModel vdm) {
                variablesTag.add(vdm.serializeNBT(provider));
            }
        }
        tag.put("variables", variablesTag);

        // 2. Portals
        var portalsTag = new ListTag();
        for (var portal : portalModels) {
            if (portal != null) {
                portalsTag.add(portal.serializeNBT(provider));
            }
        }
        tag.put("portals", portalsTag);

        // 3. Sections (with hierarchy)
        var sectionsTag = new ListTag();
        for (var section : sectionModels) {
            var sectionTag = section.serializeNBT(provider);
            sectionTag.put("items", serializeGroupItems(section, provider));
            sectionsTag.add(sectionTag);
        }
        tag.put("sections", sectionsTag);

        // 4. Nodes
        var nodesTag = new ListTag();
        for (var nodeModel : nodeModels) {
            if (nodeModel == null) continue;
            var nodeTag = nodeModel.serializeNBT(provider);
            nodeTag.putString("_type", getNodeDiscriminator(nodeModel));
            if (nodeModel instanceof ICustomNodeModel customNode && customNode.getNode() != null) {
                nodeTag.putString("nodeClass", customNode.getNode().getClass().getName());
            }
            nodesTag.add(nodeTag);
        }
        tag.put("nodes", nodesTag);

        // 5. Wires
        var wiresTag = new ListTag();
        for (var wireModel : wireModels) {
            if (wireModel != null) {
                wiresTag.add(wireModel.serializeNBT(provider));
            }
        }
        tag.put("wires", wiresTag);

        // 6. Placemats
        var placematsTag = new ListTag();
        for (var placemat : placematModels) {
            if (placemat != null) {
                placematsTag.add(placemat.serializeNBT(provider));
            }
        }
        tag.put("placemats", placematsTag);

        // 7. Sticky Notes
        var stickyNotesTag = new ListTag();
        for (var stickyNote : stickyNoteModels) {
            if (stickyNote != null) {
                stickyNotesTag.add(stickyNote.serializeNBT(provider));
            }
        }
        tag.put("stickyNotes", stickyNotesTag);

        // 8. Local Subgraphs — each entry is a full nested GraphModel NBT, tagged with the concrete
        // graph type so cross-type local subgraphs can be rebuilt on load (absent ⇒ same type as
        // the owner, for backward compatibility with pre-cross-type saves).
        if (localSubGraphs != null && !localSubGraphs.isEmpty()) {
            var localSubGraphsTag = new ListTag();
            for (var sub : localSubGraphs) {
                if (sub == null) continue;
                var subTag = sub.serializeNBT(provider);
                if (sub instanceof CustomGraphModelImpl custom) {
                    subTag.putString("graphClass", custom.getGraph().getClass().getName());
                }
                localSubGraphsTag.add(subTag);
            }
            if (!localSubGraphsTag.isEmpty()) {
                tag.put("localSubGraphs", localSubGraphsTag);
            }
        }

        return tag;
    }

    @Override
    public void deserializeAdditionalNBT(Tag tag, HolderLookup.@NotNull Provider provider) {
        if (!(tag instanceof CompoundTag compound)) return;

        // Clear existing state
        nodeModels.clear();
        onNodeModelsReset();
        wireModels.clear();
        placematModels.clear();
        stickyNoteModels.clear();
        graphVariableModels.clear();
        portalModels.clear();
        sectionModels.clear();
        elementsByUID = null;
        portWireIndex = null;
        existingVariableNames.clear();
        if (localSubGraphs != null) localSubGraphs.clear();

        // 1. Variables
        if (compound.contains("variables")) {
            var variablesTag = compound.getList("variables", Tag.TAG_COMPOUND);
            for (int i = 0; i < variablesTag.size(); i++) {
                var varTag = variablesTag.getCompound(i);
                var variable = new VariableDeclarationModel();
                variable.setGraphModel(this);
                variable.deserializeNBT(provider, varTag);
                variable.setModifiers(variable.getModifiers());
                graphVariableModels.add(variable);
                existingVariableNames.add(variable.getName());
                registerElement(variable);
            }
        }

        // 2. Portals
        if (compound.contains("portals")) {
            var portalsTag = compound.getList("portals", Tag.TAG_COMPOUND);
            for (int i = 0; i < portalsTag.size(); i++) {
                var portalTag = portalsTag.getCompound(i);
                var portal = new DeclarationModel();
                portal.setGraphModel(this);
                portal.deserializeNBT(provider, portalTag);
                portalModels.add(portal);
                registerElement(portal);
            }
        }

        // 3. Sections (with hierarchy)
        if (compound.contains("sections")) {
            var sectionsTag = compound.getList("sections", Tag.TAG_COMPOUND);
            for (int i = 0; i < sectionsTag.size(); i++) {
                var sectionTag = sectionsTag.getCompound(i);
                var section = new SectionModel();
                section.setGraphModel(this);
                section.deserializeNBT(provider, sectionTag);
                sectionModels.add(section);
                registerElement(section);
                // Rebuild hierarchy: variables and groups under this section
                if (sectionTag.contains("items")) {
                    deserializeGroupItems(section, sectionTag.getList("items", Tag.TAG_COMPOUND), provider);
                }
            }
        }

        // 3.5 Local Subgraphs — must load before nodes so SubgraphNodeModel.defineNode() can resolve them
        if (compound.contains("localSubGraphs")) {
            var listTag = compound.getList("localSubGraphs", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                var subTag = listTag.getCompound(i);
                // graphClass absent ⇒ legacy save ⇒ same type as owner (createLocalSubgraphInstance).
                GraphModel subModel;
                if (subTag.contains("graphClass")) {
                    var graphClassName = subTag.getString("graphClass");
                    Class<? extends Graph> graphType = null;
                    try {
                        var cls = Class.forName(graphClassName);
                        if (Graph.class.isAssignableFrom(cls)) {
                            graphType = cls.asSubclass(Graph.class);
                        } else {
                            LDLib2.LOGGER.error("Local subgraph graphClass {} is not a Graph subclass", graphClassName);
                        }
                    } catch (ClassNotFoundException e) {
                        LDLib2.LOGGER.error("Unknown local subgraph graphClass {} — skipping nested graph", graphClassName);
                    }
                    subModel = createLocalSubgraphInstance(graphType);
                } else {
                    subModel = createLocalSubgraphInstance();
                }
                if (subModel == null) {
                    LDLib2.LOGGER.error("Cannot instantiate local subgraph for type {} — skipping nested graph",
                            this.getClass().getName());
                    continue;
                }
                subModel.deserializeNBT(provider, subTag);
                addLocalSubgraph(subModel);
            }
        }

        // 4. Nodes
        if (compound.contains("nodes")) {
            var nodesTag = compound.getList("nodes", Tag.TAG_COMPOUND);
            for (int i = 0; i < nodesTag.size(); i++) {
                var nodeTag = nodesTag.getCompound(i);
                var type = nodeTag.getString("_type");
                try {
                    var nodeModel = createNodeFromDiscriminator(type);
                    nodeModel.setGraphModel(this);
                    nodeModel.deserializeNBT(provider, nodeTag);

                    // ICustomNodeModel: look up node class and init (covers CustomNodeModelImpl
                    // and ContextNodeModel — blocks inside a context are restored by the
                    // context itself during its own deserialize).
                    if (nodeModel instanceof ICustomNodeModel customNode) {
                        var nodeClassName = nodeTag.getString("nodeClass");
                        Node node = findNodeByClassName(nodeClassName);
                        if (node != null) {
                            customNode.initCustomNode(node);
                        } else {
                            LDLib2.LOGGER.warn("Could not find node class: {}", nodeClassName);
                        }
                    }

                    // VariableNodeModel: resolve declaration from uid
                    if (nodeModel instanceof VariableNodeModel variableNode) {
                        resolveVariableNodeDeclaration(variableNode);
                    }

                    // WirePortalModel: resolve declaration from modelUid
                    if (nodeModel instanceof WirePortalModel portalNode) {
                        resolveWirePortalDeclaration(portalNode);
                    }

                    // defineNode reconstructs all ports with deterministic UUIDs
                    if (nodeModel instanceof NodeModel nm) {
                        nm.defineNode();
                    }

                    // The fresh-spawn lifecycle hook that creates the preview model doesn't run on
                    // load, so reconcile it now that the node (and its hasNodePreview) is restored.
                    nodeModel.syncNodePreview();

                    nodeModels.add(nodeModel);
                    registerElement(nodeModel);
                } catch (Exception e) {
                    LDLib2.LOGGER.error("Failed to deserialize node of type '{}': {}", type, e.getMessage());
                }
            }
        }

        // 5. Wires - resolve port references
        if (compound.contains("wires")) {
            var wiresTag = compound.getList("wires", Tag.TAG_COMPOUND);
            for (int i = 0; i < wiresTag.size(); i++) {
                var wireTag = wiresTag.getCompound(i);
                try {
                    var wireModel = new WireModel();
                    wireModel.setGraphModel(this);
                    wireModel.deserializeNBT(provider, wireTag);

                    // Resolve port references from the serialized UUIDs
                    var fromPortUid = WireModel.getFromPortUidFromTag(wireTag);
                    var toPortUid = WireModel.getToPortUidFromTag(wireTag);

                    PortModel fromPort = fromPortUid != null && getModel(fromPortUid) instanceof PortModel p ? p : null;
                    PortModel toPort = toPortUid != null && getModel(toPortUid) instanceof PortModel p ? p : null;

                    if (fromPort == null || toPort == null) {
                        LDLib2.LOGGER.warn("Skipping wire {} with unresolvable ports (from={}, to={})",
                                wireModel.getUid(), fromPortUid, toPortUid);
                        if (fromPortUid != null || toPortUid != null) {
                            // Dump registered port UUIDs to help diagnose why resolution failed
                            var registeredPorts = getElementsByUID().entrySet().stream()
                                    .filter(e -> e.getValue() instanceof PortModel)
                                    .map(e -> e.getKey().toString())
                                    .toList();
                            LDLib2.LOGGER.warn("  Registered port UUIDs ({}): {}", registeredPorts.size(), registeredPorts);
                        }
                        continue;
                    }
                    wireModel.setPorts(toPort, fromPort);

                    wireModels.add(wireModel);
                    registerElement(wireModel);
                    if (portWireIndex != null) {
                        portWireIndex.wireAdded(wireModel);
                    }
                } catch (Exception e) {
                    LDLib2.LOGGER.error("Failed to deserialize wire: {}", e.getMessage());
                }
            }
        }

        // 6. Placemats
        if (compound.contains("placemats")) {
            var placematsTag = compound.getList("placemats", Tag.TAG_COMPOUND);
            for (int i = 0; i < placematsTag.size(); i++) {
                var pmTag = placematsTag.getCompound(i);
                var placemat = new PlacematModel();
                placemat.setGraphModel(this);
                placemat.deserializeNBT(provider, pmTag);
                placematModels.add(placemat);
                registerElement(placemat);
            }
        }

        // 7. Sticky Notes
        if (compound.contains("stickyNotes")) {
            var stickyNotesTag = compound.getList("stickyNotes", Tag.TAG_COMPOUND);
            for (int i = 0; i < stickyNotesTag.size(); i++) {
                var snTag = stickyNotesTag.getCompound(i);
                var stickyNote = new StickyNoteModel();
                stickyNote.setGraphModel(this);
                stickyNote.deserializeNBT(provider, snTag);
                stickyNoteModels.add(stickyNote);
                registerElement(stickyNote);
            }
        }

        // 8. Failed-constant sweep — for each input constant whose decode failed (codec
        // mismatch, missing codec/accessor, corrupt NBT...), drop wires terminating at that
        // port. The saved snapshot's behaviour relied on a value we couldn't reproduce, so
        // leaving the wire connected would let an upstream node feed a value that doesn't
        // match the saved state, silently corrupting downstream computation. Surface the
        // event by dropping the wire so the user can investigate.
        //
        // Output-side wires are left alone — output ports have no constant, so a decode
        // failure isn't a meaningful event for them. Type-mismatch port-drops are already
        // handled by step 5 above (the wire's referenced port UID no longer resolves).
        dropWiresOnFailedInputConstants();
    }

    protected void onNodeModelsReset() {
    }

    private void dropWiresOnFailedInputConstants() {
        for (var nodeModel : nodeModels) {
            // removeNode leaves null slots in the list (stable-index pattern); skip them.
            if (nodeModel == null) continue;
            if (!(nodeModel instanceof NodeModel nm)) continue;
            for (var entry : nm.getInputConstantsById().entrySet()) {
                var constant = entry.getValue();
                if (constant == null || !constant.isDeserializeFailed()) continue;
                var portUniqueName = entry.getKey();
                var inputPort = nm.getInputsById().get(portUniqueName);
                if (inputPort == null) continue;
                var connectedWires = new ArrayList<>(inputPort.getConnectedWires());
                if (connectedWires.isEmpty()) {
                    LDLib2.LOGGER.error("Constant for port '{}' on node {} failed to deserialize — no wires connected, value will fall back to default.",
                            portUniqueName, nodeModel.getClass().getSimpleName());
                    continue;
                }
                LDLib2.LOGGER.error("Constant for port '{}' on node {} failed to deserialize — dropping {} wire(s) terminating at this port to surface the data-loss event.",
                        portUniqueName, nodeModel.getClass().getSimpleName(), connectedWires.size());
                for (var wire : connectedWires) {
                    removeWire(wire);
                }
            }
        }
    }

    /**
     * Finds a Node instance by its class name from the supported nodes list. Public so that
     * nested-element models (e.g. {@code ContextNodeModel}) can resolve user-node classes
     * during their own deserialization.
     */
    @Nullable
    public Node findNodeByClassName(String className) {
        if (className == null || className.isEmpty()) return null;
        for (var nodeClass : getSupportNodes()) {
            if (nodeClass.getName().equals(className)) {
                try {
                    return nodeClass.getConstructor().newInstance();
                } catch (Exception e) {
                    LDLib2.LOGGER.error("Failed to instantiate node class: {}", className, e);
                }
            }
        }
        return null;
    }

    /**
     * Resolves the declaration model for a VariableNodeModel after deserialization.
     */
    protected void resolveVariableNodeDeclaration(VariableNodeModel variableNode) {
        var declUid = variableNode.getDeclarationModelUid();
        if (declUid != null && getModel(declUid) instanceof VariableDeclarationModelBase decl) {
            variableNode.setVariableDeclarationModel(decl);
        }
    }

    /**
     * Resolves the declaration model for a WirePortalModel after deserialization.
     */
    protected void resolveWirePortalDeclaration(WirePortalModel portalNode) {
        var modelUid = portalNode.getModelUid();
        if (modelUid != null && getModel(modelUid) instanceof DeclarationModel decl) {
            portalNode.setDeclarationModel(decl);
        }
    }

    // endregion

    // region Copy/Paste

    /**
     * Data record holding serialized copy/paste information.
     */
    public record CopyPasteData(CompoundTag tag, HolderLookup.Provider provider) {}

    /**
     * Copies the given elements (nodes + internal wires) into a serialized CopyPasteData.
     * Only nodes are copied; wires whose both endpoints belong to the selected set are included automatically.
     */
    public CopyPasteData copyElements(List<? extends GraphElementModel> elements, HolderLookup.Provider provider) {
        // 1. Filter to AbstractNodeModel. Block nodes are excluded — they can't be pasted as
        // top-level nodes (they need a parent context), and a block's data already travels with
        // its parent context via ContextNodeModel.serializeAdditionalNBT, so copying the context
        // is the supported path. TODO: standalone block copy that pastes into a selected context.
        var selectedNodes = elements.stream()
                .filter(e -> e instanceof AbstractNodeModel)
                .map(e -> (AbstractNodeModel) e)
                .filter(n -> !(n instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel))
                .toList();
        var selectedNodeUids = selectedNodes.stream()
                .map(GraphElementModel::getUid)
                .collect(Collectors.toSet());

        // Wires touching a block belong to the block's node, not the context. Cover block UIDs of
        // any selected context so a wire between two blocks inside the same selected context (or
        // between a selected top-level node and a block of a selected context) survives the copy.
        var coveredNodeUids = new HashSet<>(selectedNodeUids);
        for (var node : selectedNodes) {
            if (node instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel ctx) {
                for (var block : ctx.getBlocks()) {
                    if (block != null) coveredNodeUids.add(block.getUid());
                }
            }
        }

        // 2. Collect internal wires (both ports belong to selected nodes or their blocks)
        var internalWires = new ArrayList<WireModel>();
        for (var wire : wireModels) {
            if (wire == null) continue;
            var fromPort = wire.getFromPort();
            var toPort = wire.getToPort();
            if (fromPort == null || toPort == null) continue;
            if (fromPort.getNodeModel() == null || toPort.getNodeModel() == null) continue;
            if (coveredNodeUids.contains(fromPort.getNodeModel().getUid())
                    && coveredNodeUids.contains(toPort.getNodeModel().getUid())) {
                internalWires.add(wire);
            }
        }

        var tag = new CompoundTag();

        // 3. Serialize nodes
        var nodesTag = new ListTag();
        for (var node : selectedNodes) {
            var nodeTag = node.serializeNBT(provider);
            nodeTag.putString("_type", getNodeDiscriminator(node));
            if (node instanceof ICustomNodeModel customNode && customNode.getNode() != null) {
                nodeTag.putString("nodeClass", customNode.getNode().getClass().getName());
            }
            nodesTag.add(nodeTag);
        }
        tag.put("nodes", nodesTag);

        // 4. Serialize wire references using nodeUid + portUniqueName (not port UUID)
        var wiresTag = new ListTag();
        for (var wire : internalWires) {
            var wireRef = new CompoundTag();
            wireRef.putUUID("fromNodeUid", wire.getFromPort().getNodeModel().getUid());
            wireRef.putString("fromPortUniqueName", wire.getFromPort().getUniqueName());
            wireRef.putUUID("toNodeUid", wire.getToPort().getNodeModel().getUid());
            wireRef.putString("toPortUniqueName", wire.getToPort().getUniqueName());
            wiresTag.add(wireRef);
        }
        tag.put("wires", wiresTag);

        // 5. Serialize variable declarations referenced by VariableNodeModels
        var variablesTag = new ListTag();
        var seenVarUids = new HashSet<UUID>();
        for (var node : selectedNodes) {
            if (node instanceof VariableNodeModel varNode && varNode.getDeclarationModelUid() != null) {
                var declUid = varNode.getDeclarationModelUid();
                if (seenVarUids.add(declUid) && getModel(declUid) instanceof VariableDeclarationModel vdm) {
                    variablesTag.add(vdm.serializeNBT(provider));
                }
            }
        }
        tag.put("variables", variablesTag);

        // 6. Serialize portal declarations referenced by WirePortalModels
        var portalsTag = new ListTag();
        var seenPortalUids = new HashSet<UUID>();
        for (var node : selectedNodes) {
            if (node instanceof WirePortalModel portalNode && portalNode.getModelUid() != null) {
                var modelUid = portalNode.getModelUid();
                if (seenPortalUids.add(modelUid) && getModel(modelUid) instanceof DeclarationModel decl) {
                    portalsTag.add(decl.serializeNBT(provider));
                }
            }
        }
        tag.put("portals", portalsTag);

        // 7. Serialize placemats
        var placematsTag = new ListTag();
        for (var element : elements) {
            if (element instanceof PlacematModel pm) {
                placematsTag.add(pm.serializeNBT(provider));
            }
        }
        tag.put("placemats", placematsTag);

        // 8. Serialize sticky notes
        var stickyNotesTag = new ListTag();
        for (var element : elements) {
            if (element instanceof StickyNoteModel sn) {
                stickyNotesTag.add(sn.serializeNBT(provider));
            }
        }
        tag.put("stickyNotes", stickyNotesTag);

        // 9. Serialize local subgraphs referenced by any selected LOCAL SubgraphNodeModel.
        // Without this, pasting a LOCAL subgraph node either silently shares the original inner
        // graph (in-graph paste — mutating one node's subgraph corrupts the other) or dangles
        // (cross-graph paste — the destination has no matching localSubGraphs entry).
        // We carry the full nested GraphModel NBT keyed by the original subgraph uid; paste maps
        // each to a freshly-uid'd clone and rebinds the pasted SubgraphNodeModel to it.
        var localSubgraphsTag = new ListTag();
        var seenSubUids = new HashSet<UUID>();
        for (var node : selectedNodes) {
            if (node instanceof SubgraphNodeModel sub
                    && sub.getKind() == SubgraphNodeModel.Kind.LOCAL) {
                var inner = sub.getSubgraphModel();
                if (inner == null) continue;
                if (!seenSubUids.add(inner.getUid())) continue;
                var entry = new CompoundTag();
                entry.putUUID("oldUid", inner.getUid());
                entry.put("graph", inner.serializeNBT(provider));
                localSubgraphsTag.add(entry);
            }
        }
        if (!localSubgraphsTag.isEmpty()) {
            tag.put("localSubgraphs", localSubgraphsTag);
        }

        return new CopyPasteData(tag, provider);
    }

    /**
     * Pastes elements from CopyPasteData, offsetting positions.
     * Returns all newly created GraphElementModels (nodes).
     */
    public List<GraphElementModel> pasteElements(CopyPasteData data, Vector2f positionOffset) {
        return pasteElementsWithMap(data, positionOffset).elements();
    }

    /**
     * Same as {@link #pasteElements} but additionally returns the {@code oldNodeUid → newNodeModel}
     * mapping. Useful when callers need to reattach external state (e.g. wires from outside the
     * selection) to the pasted nodes — see the "extract selection to subgraph" command.
     */
    public record PasteResult(List<GraphElementModel> elements,
                              Map<UUID, AbstractNodeModel> oldToNewNodeMap) {}

    public PasteResult pasteElementsWithMap(CopyPasteData data, Vector2f positionOffset) {
        var compound = data.tag();
        var provider = data.provider();
        var result = new ArrayList<GraphElementModel>();
        var oldToNewNodeMapOuter = new HashMap<UUID, AbstractNodeModel>();
        // oldSubGraphUid → freshly-cloned inner GraphModel (added to this.localSubGraphs).
        // Built BEFORE node deserialize so SubgraphNodeModel can rebind during paste.
        var oldToNewSubgraphUid = new HashMap<UUID, UUID>();
        if (compound.contains("localSubgraphs")) {
            var listTag = compound.getList("localSubgraphs", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                var entry = listTag.getCompound(i);
                if (!entry.contains("oldUid") || !entry.contains("graph")) continue;
                var oldUid = entry.getUUID("oldUid");
                var clone = createLocalSubgraphInstance();
                if (clone == null) {
                    LDLib2.LOGGER.warn("Cannot instantiate local subgraph for paste; clone skipped");
                    continue;
                }
                clone.deserializeNBT(provider, entry.getCompound("graph"));
                // Fresh uid for the clone — uid travels via the SubgraphNodeModel.localGraphId
                // map; nothing in the inner graph references its own outer uid.
                clone.setUid(UUID.randomUUID());
                addLocalSubgraph(clone);
                oldToNewSubgraphUid.put(oldUid, clone.getUid());
            }
        }

        // 1. Variable declarations: deserialize, reuse existing by UID or register new
        if (compound.contains("variables")) {
            var variablesTag = compound.getList("variables", Tag.TAG_COMPOUND);
            for (int i = 0; i < variablesTag.size(); i++) {
                var varTag = variablesTag.getCompound(i);
                var variable = new VariableDeclarationModel();
                variable.setGraphModel(this);
                variable.deserializeNBT(provider, varTag);
                variable.setModifiers(variable.getModifiers());
                if (!hasModel(variable.getUid())) {
                    addVariableDeclaration(variable);
                }
            }
        }

        // 2. Portal declarations: same logic
        if (compound.contains("portals")) {
            var portalsTag = compound.getList("portals", Tag.TAG_COMPOUND);
            for (int i = 0; i < portalsTag.size(); i++) {
                var portalTag = portalsTag.getCompound(i);
                var portal = new DeclarationModel();
                portal.setGraphModel(this);
                portal.deserializeNBT(provider, portalTag);
                if (!hasModel(portal.getUid())) {
                    addPortal(portal);
                }
            }
        }

        // 3. Nodes: recreate with new UUIDs
        var oldToNewNodeMap = oldToNewNodeMapOuter;
        if (compound.contains("nodes")) {
            var nodesTag = compound.getList("nodes", Tag.TAG_COMPOUND);
            for (int i = 0; i < nodesTag.size(); i++) {
                var nodeTag = nodesTag.getCompound(i);
                var type = nodeTag.getString("_type");
                try {
                    var nodeModel = createNodeFromDiscriminator(type);
                    nodeModel.setGraphModel(this);
                    nodeModel.deserializeNBT(provider, nodeTag);

                    var oldUid = nodeModel.getUid();

                    // Assign new UUID
                    nodeModel.setUid(UUID.randomUUID());

                    // ContextNodeModel: also re-uid nested blocks BEFORE defineNode rebuilds
                    // ports. Blocks were just restored with their source-graph UIDs; defining
                    // ports against those would compute colliding port UIDs (same-graph paste)
                    // and prevent wires from resolving against the pasted copy instead of the
                    // original. Recording the old→new block UID mapping also lets the wire
                    // reconnect step find ports on the new block.
                    if (nodeModel instanceof ContextNodeModel ctx) {
                        for (var block : ctx.getBlocks()) {
                            if (block == null) continue;
                            var oldBlockUid = block.getUid();
                            block.setUid(UUID.randomUUID());
                            oldToNewNodeMap.put(oldBlockUid, block);
                        }
                    }

                    // ICustomNodeModel: init node class (covers CustomNodeModelImpl + ContextNodeModel)
                    if (nodeModel instanceof ICustomNodeModel customNode) {
                        var nodeClassName = nodeTag.getString("nodeClass");
                        Node node = findNodeByClassName(nodeClassName);
                        if (node != null) {
                            customNode.initCustomNode(node);
                        }
                    }

                    // VariableNodeModel: resolve declaration
                    if (nodeModel instanceof VariableNodeModel variableNode) {
                        resolveVariableNodeDeclaration(variableNode);
                    }

                    // WirePortalModel: resolve declaration
                    if (nodeModel instanceof WirePortalModel portalNode) {
                        resolveWirePortalDeclaration(portalNode);
                    }

                    // SubgraphNodeModel LOCAL: rebind to the cloned inner graph (if we cloned one
                    // for this oldUid above). EXTERNAL needs nothing — path-string is shared.
                    if (nodeModel instanceof SubgraphNodeModel subNode
                            && subNode.getKind() == SubgraphNodeModel.Kind.LOCAL
                            && subNode.getLocalGraphId() != null) {
                        var newSubUid = oldToNewSubgraphUid.get(subNode.getLocalGraphId());
                        if (newSubUid != null) {
                            subNode.rebindLocalGraphId(newSubUid);
                        }
                    }

                    // defineNode → reconstructs ports with deterministic UUIDs based on new node UUID
                    if (nodeModel instanceof NodeModel nm) {
                        nm.defineNode();
                    }

                    // Paste rebuilds the node via createNodeFromDiscriminator (node == null), so the
                    // fresh-spawn lifecycle hook that creates the preview model never runs — reconcile
                    // it now that the node (and its hasNodePreview) is restored, mirroring the load path.
                    nodeModel.syncNodePreview();

                    // Offset position
                    var pos = nodeModel.getPosition();
                    nodeModel.setPosition(new Vector2f(pos.x + positionOffset.x, pos.y + positionOffset.y));

                    addNode(nodeModel);

                    oldToNewNodeMap.put(oldUid, nodeModel);
                    result.add(nodeModel);
                } catch (Exception e) {
                    LDLib2.LOGGER.error("Failed to paste node of type '{}': {}", type, e.getMessage());
                }
            }
        }

        // 4. Wires: reconnect using oldNodeUid→newNode mapping + portUniqueName
        if (compound.contains("wires")) {
            var wiresTag = compound.getList("wires", Tag.TAG_COMPOUND);
            for (int i = 0; i < wiresTag.size(); i++) {
                var wireRef = wiresTag.getCompound(i);
                var fromNodeUid = wireRef.getUUID("fromNodeUid");
                var fromPortName = wireRef.getString("fromPortUniqueName");
                var toNodeUid = wireRef.getUUID("toNodeUid");
                var toPortName = wireRef.getString("toPortUniqueName");

                var newFromNode = oldToNewNodeMap.get(fromNodeUid);
                var newToNode = oldToNewNodeMap.get(toNodeUid);
                if (newFromNode == null || newToNode == null) continue;

                var fromPort = findPortByUniqueName(newFromNode, fromPortName);
                var toPort = findPortByUniqueName(newToNode, toPortName);
                if (fromPort != null && toPort != null) {
                    createWire(toPort, fromPort);
                }
            }
        }

        // 5. Placemats: recreate with new UUIDs and offset
        if (compound.contains("placemats")) {
            var placematsTag = compound.getList("placemats", Tag.TAG_COMPOUND);
            for (int i = 0; i < placematsTag.size(); i++) {
                var pmTag = placematsTag.getCompound(i);
                var pm = new PlacematModel();
                pm.setGraphModel(this);
                pm.deserializeNBT(provider, pmTag);
                pm.setUid(UUID.randomUUID());
                var pos = pm.getPosition();
                pm.setPosition(new Vector2f(pos.x + positionOffset.x, pos.y + positionOffset.y));
                placematModels.add(pm);
                registerElement(pm);
                getCurrentGraphChangeDescription().addNewModel(pm);
                result.add(pm);
            }
        }

        // 6. Sticky Notes: recreate with new UUIDs and offset
        if (compound.contains("stickyNotes")) {
            var stickyNotesTag = compound.getList("stickyNotes", Tag.TAG_COMPOUND);
            for (int i = 0; i < stickyNotesTag.size(); i++) {
                var snTag = stickyNotesTag.getCompound(i);
                var sn = new StickyNoteModel();
                sn.setGraphModel(this);
                sn.deserializeNBT(provider, snTag);
                sn.setUid(UUID.randomUUID());
                var pos = sn.getPosition();
                sn.setPosition(new Vector2f(pos.x + positionOffset.x, pos.y + positionOffset.y));
                stickyNoteModels.add(sn);
                registerElement(sn);
                getCurrentGraphChangeDescription().addNewModel(sn);
                result.add(sn);
            }
        }

        return new PasteResult(result, oldToNewNodeMapOuter);
    }

    /**
     * Finds a port on a node by its unique name, searching through all ports and sub-ports.
     */
    @Nullable
    public static PortModel findPortByUniqueName(AbstractNodeModel node, String uniqueName) {
        if (!(node instanceof PortNodeModel portNode)) return null;
        for (var port : portNode.getPorts()) {
            if (port.getUniqueName().equals(uniqueName)) return port;
            var found = findPortInSubPorts(port, uniqueName);
            if (found != null) return found;
        }
        return null;
    }

    @Nullable
    private static PortModel findPortInSubPorts(PortModel port, String uniqueName) {
        for (var subPort : port.getSubPorts()) {
            if (subPort.getUniqueName().equals(uniqueName)) return subPort;
            var found = findPortInSubPorts(subPort, uniqueName);
            if (found != null) return found;
        }
        return null;
    }

    // endregion

}
