package com.lowdragmc.lowdraglib.gui.graphprocessor.data;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.TriggerLink;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.utils.TypeAdapter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import oshi.util.tuples.Pair;

import java.util.*;
import java.util.function.Consumer;

public class BaseGraph implements IPersistedSerializable {
    public final HashSet<UUID> usedGUIDs = new HashSet<>();

    public UUID newGUID() {
        var guid = UUID.randomUUID();
        while (usedGUIDs.contains(guid)) {
            guid = UUID.randomUUID();
        }
        usedGUIDs.add(guid);
        return guid;
    }

    public void addGUID(String guid) {
        usedGUIDs.add(UUID.fromString(guid));
    }

    public void addGUID(UUID guid) {
        usedGUIDs.add(guid);
    }

    public int getDepth() {
        return nodes.stream().mapToInt(BaseNode::getComputeOrder).max().orElse(0);
    }

    public void resetNodes() {
        nodes.forEach(BaseNode::resetNode);
    }

    @Setter
    @Accessors(chain = true, fluent = true)
    public static class GraphChanges {
        public PortEdge	removedEdge;
        public PortEdge	addedEdge;
        public BaseNode	removedNode;
        public BaseNode	addedNode;
        public BaseNode nodeChanged;
    }

    /**
     * Compute order type used to determine the compute order integer on the nodes
     */
    public enum ComputeOrderType {
        DepthFirst,
        BreadthFirst,
    }
    
    protected static final int maxComputeOrderDepth = 1000;
    /**
     * Invalid compute order number of a node when it's inside a loop
     */
    public static final int loopComputeOrder = -2;
    /**
     * Invalid compute order number of a node can't process
     */
    public static final int invalidComputeOrder = -1;

    /**
     * List of all the nodes in the graph.
     */
    public final List<BaseNode> nodes = new ArrayList<>();

    /**
     * Map to access node per GUID, faster than a search in a list
     */
    public final Map<String, BaseNode> nodesPerGUID = new HashMap<>();

    /**
     * List of all the edges in the graph.
     */
    public final List<PortEdge> edges = new ArrayList<>();

    /**
     * Dictionary of edges per GUID, faster than a search in a list
     */
    public final Map<String, PortEdge> edgesPerGUID = new HashMap<>();

    private Map<BaseNode, Integer> computeOrderMap = new HashMap<>();

    /**
     * All exposed parameters in the graph
     */
    public final Map<String, ExposedParameter> exposedParameters = new LinkedHashMap<>();

    /**
     * Triggered when the value of an exposed parameter is updated
     */
    public Consumer<ExposedParameter> onParameterValueUpdated;

    /**
     * Triggered when the graph is changed
     */
    public Consumer<GraphChanges> onGraphChanges;

    public final Set<BaseNode> graphOutputs = new HashSet<>();

    public BaseGraph() {
    }

    public BaseGraph(List<ExposedParameter<?>> exposedParameters) {
        for (ExposedParameter<?> exposedParameter : exposedParameters) {
            this.exposedParameters.put(exposedParameter.identifier, exposedParameter);
        }
    }

    public void initialize() {
        initializeGraphElements();
        destroyBrokenGraphElements();
        updateComputeOrder(ComputeOrderType.DepthFirst);
    }


    private void initializeGraphElements() {
        // Sanitize the element lists (it's possible that nodes are null if their full class name have changed)
        // If you rename / change the assembly of a node or parameter, please use the MovedFrom() attribute to avoid breaking the graph.
        nodes.removeIf(Objects::isNull);

        nodesPerGUID.clear();
        edgesPerGUID.clear();
        for (var node : nodes) {
            node.initialize(this);
            nodesPerGUID.put(node.getGUID(), node);
        }

        for (var edge : edges) {
            edge.initialize(this);
            edgesPerGUID.put(edge.GUID, edge);

            // Sanity check for the edge:
            if (edge.inputPort == null || edge.outputPort == null) {
                disconnect(edge.GUID);
                continue;
            }

            // Add the edge to the non-serialized port data
            edge.inputPort.owner.onEdgeConnected(edge);
            edge.outputPort.owner.onEdgeConnected(edge);
        }
    }

    public void onAssetDeleted() {}

    /**
     * Adds a node to the graph
     */
    public BaseNode addNode(BaseNode node) {
        nodes.add(node);
        node.initialize(this);
        nodesPerGUID.put(node.getGUID(), node);
        if (onGraphChanges != null) {
            onGraphChanges.accept(new GraphChanges().addedNode(node));
        }
        return node;
    }

    /**
     * Removes a node from the graph
     */
    public void removeNode(BaseNode node) {
        node.disableInternal();
        node.destroyInternal();
        nodesPerGUID.remove(node.getGUID());
        nodes.remove(node);
        if (onGraphChanges != null) {
            onGraphChanges.accept(new GraphChanges().removedNode(node));
        }
    }

    public PortEdge connect(NodePort inputPort, NodePort outputPort) {
        return connect(inputPort, outputPort, true);
    }

    /**
     * Connect two ports with an edge
     */
    public PortEdge connect(NodePort inputPort, NodePort outputPort, boolean autoDisconnectInputs) {
        var edge = PortEdge.createNewEdge(this, inputPort, outputPort);
        var hasExistingEdge = edges.stream().anyMatch(e -> e.inputPort == edge.inputPort && e.outputPort == edge.outputPort);
        if (hasExistingEdge) return null;

        //If the input port does not support multi-connection, we remove them
        if (autoDisconnectInputs && !inputPort.portData.acceptMultipleEdges) {
            var copiedEdges = new ArrayList<>(inputPort.getEdges());
            for (var e : copiedEdges) {
                // TODO: do not disconnect them if the connected port is the same than the old connected
                disconnect(e);
            }
        }
        // same for the output port:
        if (autoDisconnectInputs && !outputPort.portData.acceptMultipleEdges) {
            var copiedEdges = new ArrayList<>(outputPort.getEdges());
            for (var e : copiedEdges) {
                // TODO: do not disconnect them if the connected port is the same than the old connected
                disconnect(e);
            }
        }

        edges.add(edge);
        edgesPerGUID.put(edge.GUID, edge);

        // Add the edge to the list of connected edges in the nodes
        inputPort.owner.onEdgeConnected(edge);
        outputPort.owner.onEdgeConnected(edge);
        if (onGraphChanges != null) {
            onGraphChanges.accept(new GraphChanges().addedEdge(edge));
        }
        return edge;
    }

    /**
     * Disconnect two ports
     */
    public void disconnect(BaseNode inputNode, String inputFieldName, BaseNode outputNode, String outputFieldName) {
        edges.removeIf(r -> {
            var remove = r.inputNode == inputNode
            && r.outputNode == outputNode
            && Objects.equals(r.outputFieldName, outputFieldName)
            && Objects.equals(r.inputFieldName, inputFieldName);

            if (remove) {
                if (r.inputNode != null)
                    r.inputNode.onEdgeDisconnected(r);
                if (r.outputNode != null)
                    r.outputNode.onEdgeDisconnected(r);
                if (onGraphChanges != null)
                    onGraphChanges.accept(new GraphChanges().removedEdge(r));
            }

            return remove;
        });
    }

    /**
     * Disconnect an edge
     */
    public void disconnect(PortEdge edge) {
        disconnect(edge.GUID);
    }

    /**
     * Disconnect an edge
     */
    public void disconnect(String edgeGUID) {
        var disconnectEvents = new ArrayList<Pair<BaseNode, PortEdge>>();

        edges.removeIf(r -> {
            if (Objects.equals(r.GUID, edgeGUID)) {
                disconnectEvents.add(new Pair<>(r.inputNode, r));
                disconnectEvents.add(new Pair<>(r.outputNode, r));
                if (onGraphChanges != null) {
                    onGraphChanges.accept(new GraphChanges().removedEdge(r));
                }
            }
            return Objects.equals(r.GUID, edgeGUID);
        });

        // Delay the edge disconnect event to avoid recursion
        for (var tuple : disconnectEvents) {
            if (tuple.getA() != null) {
                tuple.getA().onEdgeDisconnected(tuple.getB());
            }
        }
    }

    public ExposedParameter<?> getExposedParameterFromIdentifier(String parameterIdentifier) {
        return exposedParameters.get(parameterIdentifier);
    }

    public void updateExposedParameter(String identifier, Object input) {
        var parameter = exposedParameters.get(identifier);
        if (parameter != null) {
            parameter.setValue(input);
            if (onParameterValueUpdated != null) {
                onParameterValueUpdated.accept(parameter);
            }
        }
    }

    /**
     * Invoke the onGraphChanges event, can be used as trigger to execute the graph when the content of a node is changed
     */
    public void notifyNodeChanged(BaseNode node) {
        if (onGraphChanges != null) {
            onGraphChanges.accept(new GraphChanges().nodeChanged(node));
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = IPersistedSerializable.super.serializeNBT();
        var nodes = new ListTag();
        for (var node : this.nodes) {
            nodes.add(node.serializeNBT());
        }
        // save nodes
        tag.put("nodes", nodes);
        var edges = new ListTag();
        for (var edge : this.edges) {
            edges.add(edge.serializeNBT());
        }
        // save edges
        tag.put("edges", edges);
        // save parameters
        var parameters = new CompoundTag();
        for (var parameter : exposedParameters.values()) {
            var identifier = parameter.identifier;
            parameters.put(identifier, parameter.serializeNBT());
        }
        tag.put("parameters", parameters);
        return tag;
    }

    /**
     * We can deserialize data here and load objects references
     */
    @Override
    public void deserializeNBT(CompoundTag tag) {
        nodes.removeIf(Objects::isNull);
        if (!nodes.isEmpty()) {
            for (var node : nodes)
                node.disableInternal();
        }
        nodes.clear();
        edges.clear();
        // load nodes
        IPersistedSerializable.super.deserializeNBT(tag);
        var nodes = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < nodes.size(); i++) {
            this.nodes.add(BaseNode.createFromTag(nodes.getCompound(i)));
        }
        // load edges
        var edges = tag.getList("edges", Tag.TAG_COMPOUND);
        for (int i = 0; i < edges.size(); i++) {
            var edge = new PortEdge();
            edge.deserializeNBT(edges.getCompound(i));
            this.edges.add(edge);
        }
        // load parameters
        var parameters = tag.getCompound("parameters");
        for (var parameter : exposedParameters.values()) {
            var identifier = parameter.identifier;
            if (parameters.contains(identifier)) {
                parameter.deserializeNBT(parameters.getCompound(identifier));
            }
        }
        initialize();
    }

    /**
     * Update the compute order of the nodes in the graph
     */
    public void updateComputeOrder(ComputeOrderType type) {
        if (nodes.isEmpty() )
            return ;

        // Find graph outputs (end nodes) and reset compute order
        graphOutputs.clear();
        for (var node : nodes) {
            if (node.GetOutputNodes().isEmpty())
                graphOutputs.add(node);
            node.computeOrder = 0;
        }

        computeOrderMap.clear();
        infiniteLoopTracker.clear();

        if (type == ComputeOrderType.BreadthFirst) {
            for (var node : nodes) updateComputeOrderBreadthFirst(0, node);
        } else if (type == ComputeOrderType.DepthFirst) {
            updateComputeOrderDepthFirst();
        }
    }

    protected final HashSet<BaseNode> infiniteLoopTracker = new HashSet<>();

    protected int updateComputeOrderBreadthFirst(int depth, BaseNode node) {
        int computeOrder = 0;

        if (depth > maxComputeOrderDepth) {
            LDLib.LOGGER.error("Recursion error while updating compute order");
            return -1;
        }

        if (computeOrderMap.containsKey(node))
            return node.computeOrder;

        if (!infiniteLoopTracker.add(node))
            return -1;

        if (!node.canProcess)
        {
            node.computeOrder = -1;
            computeOrderMap.put(node, -1);
            return -1;
        }

        for (var dep : node.getInputNodes()) {
            int c = updateComputeOrderBreadthFirst(depth + 1, dep);

            if (c == -1) {
                computeOrder = -1;
                break ;
            }

            computeOrder += c;
        }

        if (computeOrder != -1)
            computeOrder++;

        node.computeOrder = computeOrder;
        computeOrderMap.put(node, computeOrder);

        return computeOrder;
    }

    protected void updateComputeOrderDepthFirst() {
        Stack<BaseNode> dfs = new Stack<BaseNode>();

        GraphUtils.FindCyclesInGraph(this, n -> propagateComputeOrder(n, loopComputeOrder));

        int computeOrder = 0;
        for (var node : GraphUtils.DepthFirstSort(this)) {
            if (node.computeOrder == loopComputeOrder)
                continue;
            if (!node.canProcess)
                node.computeOrder = -1;
            else
                node.computeOrder = computeOrder++;
        }
    }

    protected void propagateComputeOrder(BaseNode node, int computeOrder) {
        Stack<BaseNode> deps = new Stack<>();
        HashSet<BaseNode> loop = new HashSet<>();

        deps.push(node);
        while (!deps.isEmpty()) {
            var n = deps.pop();
            n.computeOrder = computeOrder;
            if (!loop.add(n))
                continue;
            for (var dep : n.GetOutputNodes()) deps.push(dep);
        }
    }

    void destroyBrokenGraphElements() {
        edges.removeIf(e -> e.inputNode == null
                || e.outputNode == null
                || (e.outputFieldName == null || e.outputFieldName.isEmpty())
                || (e.inputFieldName == null || e.inputFieldName.isEmpty()));
        nodes.removeIf(Objects::isNull);
    }

    /**
     * Tell if two types can be connected in the context of a graph
     */
    public static boolean areTypesConnectable(Class from, Class to) {
        if (from == TriggerLink.class || to == TriggerLink.class) {
            return from == to;
        }

        if (from == null || to == null)
            return false;

        if (TypeAdapter.areIncompatible(from, to))
            return false;

//        //Check if there is custom adapters for this assignation
//        if (CustomPortIO.IsAssignable(from, to))
//            return true;

        //Check for type assignability
        if (to.isAssignableFrom(from))
            return true;

        if (to == Object.class)
            return true;

        if (from == UnknownType.class || from == Object.class)
            return true;

        // User defined type convertions
        return TypeAdapter.areConvertable(from, to);
    }
}
