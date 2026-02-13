package com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortCapacity;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.utils.ReorderType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.TypeConstant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.SubPortDefinitionScope;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.PlacematModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.StickyNoteModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WirePlaceHolder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireSide;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The model that represents a graph's structure and contents.
 *
 * <p>GraphModel manages nodes, wires, variables, and other graph elements.
 * It also tracks changes for efficient UI updates.</p>
 */
public abstract class GraphModel extends GraphElementModel implements IGraphElementContainer {
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

    private GraphChangeDescription currentChangeDescription = new GraphChangeDescription();

    // runtime
    @Nullable
    private Map<UUID, GraphElementModel> elementsByUID;

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
     * Whether the graph is a state machine graph.
     */
    public boolean isStateMachineGraph() {
        return false;
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
    public boolean repair() {
        return false;
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
        // todo others
//        foreach (var model in m_GraphStickyNoteModels)
//        {
//            RegisterElement(model);
//        }
//
//        foreach (var model in m_GraphPlacematModels)
//        {
//            RegisterElement(model);
//        }
//
//        // Some variables may not be under any section.
//        foreach (var model in m_GraphVariableModels)
//        {
//            RegisterElement(model);
//        }
//
        portalModels.forEach(this::registerElement);
//
//        foreach (var model in m_SectionModels)
//        {
//            RegisterElement(model);
//        }
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

        for (var subModel : model.getDependentModels()) {
            registerElement(subModel);
        }
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
        for (var subModel : model.getDependentModels()) {
            unregisterElement(subModel);
        }
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
//        var initialVariables = new HashSet<VariableDeclarationModelBase>(VariableDeclarations.Where(v => v != null && v.IsInputOrOutput));
        var elementsByType = new ElementsByType(graphElementModels);

        // Add nodes that would be backed by declaration models.
//        elementsByType.NodeModels.UnionWith(elementsByType.VariableDeclarationsModels.SelectMany(FindReferencesInGraph<AbstractNodeModel>));

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

//        deleteVariableDeclarations(elementsByType.VariableDeclarationsModels, deleteUsages: false);
//        deleteGroups(elementsByType.GroupModels);
//        deleteStickyNotes(elementsByType.StickyNoteModels);
//        deletePlacemats(elementsByType.PlacematModels);
        deleteWires(elementsByType.wireModels);
        deleteNodes(elementsByType.nodeModels, false, true);

//        if (elementsByType.VariableDeclarationsModels.Count > 0)
//        {
//            // Find out if there were any deleted I/O variable declaration.
//            foreach (var variableDeclaration in VariableDeclarations)
//            {
//                if (variableDeclaration != null && variableDeclaration.IsInputOrOutput)
//                {
//                    initialVariables.Remove(variableDeclaration);
//                }
//            }
//
//            if (initialVariables.Count > 0)
//            {
//                foreach (var recursiveSubgraphNode in GetSelfReferringSubgraphNodes())
//                recursiveSubgraphNode.Update();
//            }
//        }
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
    protected void removeElements(List<? extends GraphElementModel> elements) {
        for (var element : elements) {
            switch (element) {
                case IPlaceHolder placeHolder:
                    removePlaceholder(placeHolder);
                    break;
//                case StickyNoteModel stickyNoteModel:
//                    RemoveStickyNote(stickyNoteModel);
//                    break;
//                case PlacematModel placematModel:
//                    RemovePlacemat(placematModel);
//                    break;
//                case VariableDeclarationModelBase variableDeclarationModel:
//                    RemoveVariableDeclaration(variableDeclarationModel);
//                    break;
                case WireModel wireModel:
                    removeWire(wireModel);
                    break;
//                case BlockNodeModel blockNodeModel:
//                    UnregisterBlockNode(blockNodeModel);
//                    break;
                case AbstractNodeModel nodeModel:
                    removeNode(nodeModel);
                    break;
                case PortModel portModel:
                    unregisterPort(portModel);
                    break;
//                case SectionModel sectionModel:
//                    RemoveSection(sectionModel);
//                    break;
//                case GraphModel gm:
//                    removeGroup(gm);
//                    break;
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

        var instance = new TypeConstant(t);
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
                nodeModel.getValue().init(constantType);
                if (initializationCallback != null) initializationCallback.accept(nodeModel);
            }
        }, spawnFlags);
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
            constantNodeModel.setValue(constant);
            nodeModel = constantNodeModel;
        } else if (AbstractNodeModel.class.isAssignableFrom(nodeType)) {
            try {
                nodeModel = (AbstractNodeModel) nodeType.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate node model of type " + nodeType.getName(), e);
            }
        } else throw new IllegalArgumentException("nodeType must be a subclass of AbstractNodeModel");

        nodeModel.setGraphModel(this);
        nodeModel.setTitle(Component.translatable(nodeName));

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


    // region Declaration / Portal

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

        MutableComponent portalName ;
        if (nodeModel instanceof ConstantNodeModel constantNodeModel) {
            portalName = Component.literal(TypeHandleHelpers.identificationOf(constantNodeModel.getType()));
        } else {
            portalName = nodeModel.getTitle().copy();
            var portName = outputPortModel.getDisplayName();
            if (portName != null) {
                portalName.append(Component.literal(" - ").append(portName));
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
    public DeclarationModel createGraphPortalDeclaration(Component portalName,
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
    protected DeclarationModel instantiatePortalDeclaration(Component title, @Nullable UUID uid) {
        if (!allowPortalCreation()) throw new IllegalArgumentException("Wire portal creation is disabled.");

        var portalModel = new DeclarationModel();
        portalModel.setTitle(title);
        if (uid != null) portalModel.setUid(uid);
        portalModel.setGraphModel(this);
        return portalModel;
    }

    // endregion

    // region sub graph

    public void removeLocalSubgraph(GraphModel subgraphModel) {
        if (localSubGraphs != null) {
            localSubGraphs.remove(subgraphModel);
            // todo subgraph
        }
    }

    // endregion

}
