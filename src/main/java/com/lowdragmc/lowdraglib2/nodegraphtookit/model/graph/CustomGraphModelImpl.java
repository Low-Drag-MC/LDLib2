package com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.GraphNodeCreationData;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.*;
import com.lowdragmc.lowdraglib2.utils.TypeUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class CustomGraphModelImpl extends GraphModel {
    @Getter
    private final Graph graph;
    // runtime
    @Nullable
    private List<Class<? extends Node>> getSupportNodes;
    @Nullable
    private List<TypeHandle> supportedTypes;
    private List<INode> nodes;


    public CustomGraphModelImpl(Graph graph) {
        this.graph = graph;
    }

    @Override
    public List<Class<? extends Node>> getSupportNodes() {
        if (getSupportNodes == null) initializeSupportedNodes();
        return getSupportNodes;
    }

    protected void initializeSupportedNodes() {
        getSupportNodes = graph.getSupportNodes();
        if (getSupportNodes == null) {
            getSupportNodes = detectSupportedNodes(this);
        }
    }

    public static List<Class<? extends Node>> detectSupportedNodes(GraphModel graphModel) {
        // TODO annotations
        return new ArrayList<>();
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

    public static List<TypeHandle> detectSupportedTypes(GraphModel graphModel) {
        var foundTypes = new HashSet<TypeHandle>();
        var nodeCreationData = GraphNodeCreationData.ofOrphan(graphModel);
        for (var nodeType : graphModel.getSupportNodes()) {
            // context node
//            if (typeof(ContextNode).IsAssignableFrom(type))
//            {
//                InitializeSupportedTypesFromContextNodeType(m_Graph.GetType(), nodeCreationData, type, supportedTypes);
//                createdElement = (IUserNodeModelImp)(CreateContextNodeFromData(nodeCreationData, type) as ContextNodeModel);
//            }
//            else
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
        return nodeCreationData.createNode(getNodeImpType(customNodeType), "", n -> {
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

    public static <T extends AbstractNodeModel & ICustomNodeModel> Class<T> getNodeImpType(Class<? extends Node> nodeType) {
//        if (ContextNode.IsAssignableFrom(nodeType)) {
//            return typeof(UserContextNodeModelImp);
//        } if (typeof(BlockNode).IsAssignableFrom(nodeType)) {
//            return typeof(UserBlockNodeModelImp);
//        }
        return (Class<T>) CustomNodeModelImpl.class;
    }

    public List<INode> getNodes() {
        if (nodes == null) buildNodesFromNodeModels();
        return nodes;
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
                    n.getValue().setDefaultValue(defaultValue);
                    n.getValue().setValue(defaultValue);
                },
                null);
    }
}
