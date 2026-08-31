package com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphLogger;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.IVariable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.GraphNodeCreationData;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableScope;
import com.lowdragmc.lowdraglib2.utils.TypeUtils;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class CustomGraphModelImpl extends GraphModel {
    @Getter
    private final Graph graph;
    // runtime
    @Nullable
    private List<Class<? extends Node>> supportNodeCache;
    @Nullable
    private List<TypeHandle> supportedTypes;
    private List<INode> nodes;


    public CustomGraphModelImpl(Graph graph) {
        this.graph = graph;
    }

    @Override
    public List<Class<? extends Node>> getSupportNodes() {
        if (supportNodeCache == null) initializeSupportedNodes();
        return supportNodeCache;
    }

    protected void initializeSupportedNodes() {
        supportNodeCache = graph.getSupportNodes();
    }

    @Override
    public @NotNull List<TypeHandle> getSupportTypes() {
        if (supportedTypes == null) initializeSupportedTypes();
        return supportedTypes;
    }

    protected void initializeSupportedTypes() {
        supportedTypes = graph.getSupportTypes();
        if (supportedTypes == null) {
            supportedTypes = detectSupportedTypes(this);
        }
    }

    @Override
    public List<Class<? extends Node>> getLibrarySupportNodes() {
        return graph.getLibrarySupportNodes();
    }

    @Override
    public List<TypeHandle> getLibrarySupportTypes() {
        var types = graph.getLibrarySupportTypes();
        return types == null ? getSupportTypes() : types;
    }

    @Override
    public List<TypeHandle> getVariableSupportTypes() {
        var types = graph.getVariableSupportTypes();
        return types == null ? getSupportTypes() : types;
    }

    @Override
    public Set<VariableKind> getSupportedSubgraphVariableKinds() {
        return graph.getSupportedSubgraphVariableKinds();
    }

    /**
     * Cached per graph type — the answer depends only on {@link GraphModel#getSupportNodes()}, which
     * is a constant of the graph class, never of the instance being asked.
     */
    private static final Map<Class<?>, List<TypeHandle>> SUPPORTED_TYPES = new ConcurrentHashMap<>();

    /**
     * Every type a port of this kind of graph can carry, found by instantiating each registered node
     * class and reading its ports.
     *
     * <h2>Cached, and isolated from the graph it is asked about</h2>
     * This is a query, but the only way to answer it is to build one of everything — several hundred
     * nodes and several thousand ports for a graph whose registry is large. Two things follow, and
     * both were live bugs:
     *
     * <ul>
     *   <li>The probe nodes are spawned orphaned and discarded, but their ports were still reported
     *       through the model's change description, so a single call queued thousands of new models
     *       for a view that has no elements for them. {@link GraphModel#withIsolatedChanges} keeps
     *       that churn off the live change set.</li>
     *   <li>Callers include the blackboard's variable-type picker, whose search runs on a background
     *       thread ({@code SearchEngine}). Building models there raced the render thread's
     *       {@code GraphView.updateGraphModelChanges}, corrupting the change set's {@code HashSet} —
     *       a {@code ConcurrentModificationException} or a {@code toArray} overflow, both of which
     *       took down the screen. Caching means the probe runs once, and
     *       {@link java.util.concurrent.ConcurrentHashMap#computeIfAbsent} serialises even that.</li>
     * </ul>
     *
     * <p>Keyed by the {@code Graph}'s class where there is one: two graph types can share a model
     * class ({@code CustomGraphModelImpl}) while registering completely different node sets.</p>
     */
    public static List<TypeHandle> detectSupportedTypes(GraphModel graphModel) {
        var key = graphModel instanceof CustomGraphModelImpl custom && custom.getGraph() != null
                ? custom.getGraph().getClass()
                : graphModel.getClass();
        return SUPPORTED_TYPES.computeIfAbsent(key, ignored ->
                graphModel.withIsolatedChanges(() -> probeSupportedTypes(graphModel)));
    }

    private static List<TypeHandle> probeSupportedTypes(GraphModel graphModel) {
        var foundTypes = new HashSet<TypeHandle>();
        var nodeCreationData = GraphNodeCreationData.ofOrphan(graphModel);
        // Iterate every registered node type (regular, context, AND block) — getNodeImplType
        // selects the right model class per nodeType, and we harvest port types uniformly.
        // Block classes appear here too because they're @NodeAttribute-registered, so their
        // port types are picked up automatically without a special context-traversal step.
        for (var nodeType : graphModel.getSupportNodes()) {
            var createdElement = createNodeFromData(nodeCreationData, nodeType);
            if (createdElement instanceof ICustomNodeModel customNodeModel) {
                getPortTypesFromNode(customNodeModel.getNode(), foundTypes);
            }
        }
        return foundTypes.stream().sorted(TypeHandle::compareTo).toList();
    }

    @Override
    public boolean canAssignTo(PortModel destination, PortModel source) {
        if (destination.getDataTypeHandle().equals(TypeHandles.EXECUTION_FLOW)) {
            return source.getDataTypeHandle().equals(TypeHandles.EXECUTION_FLOW);
        }
        return TypeUtils.isAssignableFrom(destination.getPortDataType(), source.getPortDataType());
    }

    public NodeModel createNodeModel(Node node, Vector2f position) {
        return createNodeWithType(CustomNodeModelImpl.class, "", position, null,
                n -> n.initCustomNode(node), null);
    }

    public static AbstractNodeModel createNodeFromData(GraphNodeCreationData nodeCreationData, Class<? extends Node> customNodeType) {
        return nodeCreationData.createNode(getNodeImplType(customNodeType), "", n -> {
           if (n instanceof ICustomNodeModel customNodeModel) {
               try {
                   customNodeModel.initCustomNode(customNodeType.getConstructor().newInstance());
               } catch (Exception e) {
                   LDLib2.LOGGER.error("Failed to instantiate custom node {}", customNodeType.getName(), e);
                   throw new RuntimeException(e);
               }
           }
        });
    }

    public static void getPortTypesFromNode(INode node, Set<TypeHandle> portTypes) {
        if (node == null) return;
        if (portTypes == null) return;
        Stream.concat(node.getInputPorts().stream(), node.getOutputPorts().stream()).forEach(port -> {
            var dataTypeHandle = port.getDataTypeHandle();
            if (dataTypeHandle != null) {
                portTypes.add(dataTypeHandle);
            } else {
                var dataType =port.getDataType();
                if (dataType != null) {
                    portTypes.add(TypeHandleHelpers.fromType(dataType));
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractNodeModel & ICustomNodeModel> Class<T> getNodeImplType(Class<? extends Node> nodeType) {
        if (ContextNode.class.isAssignableFrom(nodeType)) {
            return (Class<T>) CustomContextNodeModelImpl.class;
        }
        if (BlockNode.class.isAssignableFrom(nodeType)) {
            return (Class<T>) CustomBlockNodeModelImpl.class;
        }
        return (Class<T>) CustomNodeModelImpl.class;
    }

    @Override
    protected void onNodeModelsReset() {
        nodes = null;
    }

    public List<? extends INode> getNodes() {
        if (nodes == null) buildNodesFromNodeModels();
        return nodes;
    }

    public List<? extends IVariable> getVariableModels() {
        return this.getGraphVariableModels();
    }

    protected void buildNodesFromNodeModels() {
        nodes = new ArrayList<>(getNodeModels().size());
        for (var nodeModel : getNodeModels()) {
            addNodeFromNodeModel(nodeModel);
        }
    }

    protected void addNodeFromNodeModel(AbstractNodeModel nodeModel) {
        if(nodeModel instanceof ICustomNodeModel customNodeModel) {
            nodes.add(customNodeModel.getNode());
        } else if(nodeModel instanceof INode node){
            nodes.add(node);
        }
    }

    @Override
    protected void addNode(AbstractNodeModel nodeModel) {
        if (nodes == null) buildNodesFromNodeModels();
        super.addNode(nodeModel);
        addNodeFromNodeModel(nodeModel);
    }

    @Override
    protected void removeNode(AbstractNodeModel nodeModel) {
        if (nodes != null) {
            if (nodeModel instanceof ICustomNodeModel customNodeModel) {
                nodes.remove(customNodeModel.getNode());
            } else if (nodeModel instanceof INode node) {
                nodes.remove(node);
            }
        }
        super.removeNode(nodeModel);
    }

    @Override
    protected ConstantNodeModel newConstantNodeModel() {
        return new ConstantNodeModelImpl();
    }

    public IConstantNode createConstantNode(String name,
                                            Vector2f position,
                                            TypeHandle valueType,
                                            @Nullable Object defaultValue) {
        return (ConstantNodeModelImpl) createConstantNode(valueType, name, position,
                null,
                n -> {
                    n.getConstant().setDefaultValue(defaultValue);
                    n.getConstant().setValue(defaultValue);
                },
                null);
    }

    @Override
    protected Class<? extends VariableNodeModel> getVariableNodeType() {
        return VariableNodeModelImpl.class;
    }

    public IVariable createVariable(String name, Type valueType, @Nullable Object defaultValue, @Nullable VariableKind kind) {
        return createVariable(name, TypeHandleHelpers.fromType(valueType), defaultValue, kind);
    }

    public IVariable createVariable(String name, TypeHandle valueType, @Nullable Object defaultValue, @Nullable VariableKind kind) {
        var variableKind = kind == null ? VariableKind.LOCAL : kind;
        if (variableKind != VariableKind.LOCAL && !supportsSubgraphVariableKind(variableKind)) {
            variableKind = VariableKind.LOCAL;
        }
        var constant = createConstantValue(valueType);
        if (defaultValue != null) {
            constant.setDefaultValue(defaultValue);
            constant.setValue(defaultValue);
        }

        return createGraphVariableDeclaration(
                valueType,
                name,
                variableKind == VariableKind.INPUT ? ModifierFlags.READ : (variableKind == VariableKind.OUTPUT ? ModifierFlags.WRITE : ModifierFlags.NONE),
                variableKind != VariableKind.LOCAL ? VariableScope.EXPOSED : VariableScope.LOCAL,
                null,
                Integer.MAX_VALUE,
                constant,
                null, null
        );
    }

    @Override
    public boolean variableDeclarationRequiresInitialization(VariableDeclarationModelBase decl) {
        // We want all variables to have a default value field.
        return true;
    }

    @Override
    public boolean canExecuteCommand(com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.IGraphCommand command) {
        return graph.canExecuteCommand(command);
    }

    @Override
    public void onCommandExecuted(com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.IGraphCommand command) {
        graph.onCommandExecuted(command);
    }

    @Override
    public void onGraphChanged(GraphLogger logger) {
        super.onGraphChanged(logger); // built-in diagnostics (missing ports)
        graph.onGraphChanged(logger);
    }

    @Override
    public CustomGraphModelImpl createLocalSubgraphInstance() {
        return createLocalSubgraphInstance(graph.getClass());
    }

    @Override
    public CustomGraphModelImpl createLocalSubgraphInstance(@Nullable Class<? extends Graph> graphType) {
        var type = graphType != null ? graphType : graph.getClass();
        try {
            var newGraph = type.getDeclaredConstructor().newInstance();
            // Same-type subgraphs are always allowed; cross-type must be opted into by the host
            // graph via acceptsSubgraphGraph.
            if (type != graph.getClass() && !graph.acceptsSubgraphGraph(newGraph)) {
                LDLib2.LOGGER.warn("Graph type {} does not accept subgraph of type {}",
                        graph.getClass().getName(), type.getName());
                return null;
            }
            return newGraph.graphModel;
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to instantiate local subgraph of type {}", type.getName(), e);
            return null;
        }
    }
}
