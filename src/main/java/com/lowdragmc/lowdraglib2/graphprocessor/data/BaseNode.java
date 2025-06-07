package com.lowdragmc.lowdraglib2.graphprocessor.data;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.CustomPortBehavior;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib2.graphprocessor.data.custom.ICustomPortBehaviorDelegate;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Getter
public abstract class BaseNode implements IPersistedSerializable, ILDLRegister<BaseNode, Supplier<BaseNode>>, IConfigurable {
    public final static Codec<BaseNode> CODEC = LDLib2Registries.GRAPH_NODES.optionalCodec().dispatch(ILDLRegister::getRegistryHolderOptional,
            optional -> optional.map(holder -> PersistedParser.createCodec(holder.value()).fieldOf("data"))
                    .orElseGet(LDLibExtraCodecs::errorDecoder));

    /**
     * Name of the node, it will be displayed in the title section
     */
    @Persisted
    @Setter
    protected String displayName = name();
    /**
     * The accent color of the node title
     */
    protected int titleColor = ColorPattern.GRAY.color;
    /**
     * The GUID of the node, which is used to identify the node
     */
    @Persisted
    private String GUID;
    /**
     * The process order of the node in the graph, it should be updated by the graph.
     */
    @Persisted
    protected int computeOrder = -1;
    /**
     * Visualization configs
     */
    @Persisted
    @Setter
    public Position position;
    @Persisted
    @Setter
    public boolean expanded = true;
    @Persisted
    @Setter
    public boolean canBeRemoved = true;
    /**
     * Tell whether the node can be processed.
     * <br>
     * Do not check anything from inputs because this step happens before inputs are sent to the node
     */
    protected boolean canProcess = true;
    protected BaseGraph graph;
    /**
     * Container of input ports
     */
    public final NodePortContainer.NodeInputPortContainer inputPorts = new NodePortContainer.NodeInputPortContainer(this);
    /**
     * Container of output ports
     */
    public final NodePortContainer.NodeOutputPortContainer outputPorts = new NodePortContainer.NodeOutputPortContainer(this);
    /**
     * Triggered after the node was processed
     */
    public Runnable onProcessed;
    /**
     * Triggered after an edge was connected on the node
     */
    public Consumer<PortEdge> onAfterEdgeConnected;
    /**
     * Triggered after an edge was disconnected on the node
     */
    public Consumer<PortEdge> onAfterEdgeDisconnected;
    /**
     * Triggered after a single/list of port(s) is updated, the parameter is the field name
     */
    public Consumer<String> onPortsUpdated;

    private final Map<String, NodeFieldInformation> nodeFields = new LinkedHashMap<>();


    private record PortUpdate(List<String> fieldNames, BaseNode node) {
    }

    // Used in port update algorithm
    private final Stack<PortUpdate> fieldsToUpdate = new Stack<>();
    private final HashSet<PortUpdate> updatedFields = new HashSet<>();

    protected BaseNode() {
        InitializeInOutDatas();
    }

    public CompoundTag serializeWrapper() {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElse(new CompoundTag());
    }

    @Nullable
    public static BaseNode createFromTag(Tag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
    }

    public BaseNode copy() {
        var newNode = createFromTag(serializeWrapper());
        newNode.GUID = null;
        return newNode;
    }

    /**
     * Create a node of a certain type at a certain position
     *
     * @param nodeType type of the node
     * @param position position in the graph in pixels
     */
    @Nullable
    public static <T extends BaseNode> T createFromType(Class<T> nodeType, Position position) {
        if (!BaseNode.class.isAssignableFrom(nodeType))
            return null;
        try {
            var node = nodeType.getConstructor().newInstance();
            node.position = position;
            return node;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the minimum width of the node in the editor
     */
    public int getMinWidth() {
        return 50;
    }

    /**
     * Called only when the node is created, not when instantiated
     */
    public void newGuid(BaseGraph graph) {
        GUID = graph.newGUID().toString();
    }

    public void disableInternal() {
        // port containers are initialized in the OnEnable
        inputPorts.clear();
        outputPorts.clear();
        disable();
    }

    public void destroyInternal() {
        destroy();
    }

    public Field[] getAllFields() {
        return getClass().getFields();
    }

    public Method[] getAllMethods() {
        return getClass().getMethods();
    }

    /**
     * Detect input and output fields of the node
     */
    protected void InitializeInOutDatas() {
        var fields = getAllFields();
        var methods = getAllMethods();

        var fieldInformations = new ArrayList<NodeFieldInformation>();
        for (var field : fields) {
            var inputPort = field.isAnnotationPresent(InputPort.class) ? field.getAnnotation(InputPort.class) : null;
            var outputPort = field.isAnnotationPresent(OutputPort.class) ? field.getAnnotation(OutputPort.class) : null;
            var isMultiple = false;
            var input = false;
            var name = field.getName();
            var color = -1;
            var priority = 0;
            String[] tooltips = null;

            if (inputPort == null && outputPort == null) continue;
            if (inputPort != null && outputPort != null) {
                LDLib2.LOGGER.error("Field " + field.getName() + " cannot be both input and output");
                continue;
            }

            //check if field is a collection type
            input = inputPort != null;
            isMultiple = input ? inputPort.allowMultiple() : outputPort.allowMultiple();
            tooltips = input ? inputPort.tips() : outputPort.tips();

            if (input) {
                name = inputPort.name().isEmpty() ? name : inputPort.name();
                color = inputPort.color();
                priority = inputPort.priority();
            } else {
                name = outputPort.name().isEmpty() ? name : outputPort.name();
                color = outputPort.color();
                priority = outputPort.priority();
            }

            // By default we set the behavior to null, if the field have a custom behavior, it will be set in the loop just below
            fieldInformations.add(new NodeFieldInformation(field, name, color, input, isMultiple, tooltips, priority, null));
        }

        fieldInformations.sort(Comparator.comparingInt(f -> f.priority));
        fieldInformations.forEach(f -> nodeFields.put(f.info.getName(), f));

        for (var method : methods) {
            if (method.isAnnotationPresent(CustomPortBehavior.class)) {
                var customPortBehavior = method.getAnnotation(CustomPortBehavior.class);
                var fieldName = customPortBehavior.field();
                if (nodeFields.containsKey(fieldName)) {
                    var fieldInfo = nodeFields.get(fieldName);
                    method.setAccessible(true);
                    fieldInfo.behavior = edges -> {
                        try {
                            return (List<PortData>) method.invoke(this, edges);
                        } catch (Exception e) {
                            throw new RuntimeException("Error while invoking custom port behavior", e);
                        }
                    };
                } else {
                    LDLib2.LOGGER.error("Invalid field name for custom port behavior: " + method + ", " + customPortBehavior.field());
                }
            }
        }
    }

    public void onEdgeConnected(PortEdge edge) {
        var input = edge.inputNode == this;
        var portCollection = (input) ? inputPorts : outputPorts;
        portCollection.add(edge);
        updateAllPorts();
        if (onAfterEdgeConnected != null)
            onAfterEdgeConnected.accept(edge);
    }

    protected boolean canResetPort(NodePort port) {
        return true;
    }

    public void onEdgeDisconnected(PortEdge edge) {
        if (edge == null)
            return ;

        var input = edge.inputNode == this;
        var portCollection = (input) ? inputPorts : outputPorts;

        portCollection.remove(edge);

        // Reset default values of input port:
        var haveConnectedEdges = edge.inputNode.inputPorts.stream()
                .filter(p -> Objects.equals(p.fieldName, edge.inputFieldName))
                .anyMatch(p -> !p.getEdges().isEmpty());
        if (edge.inputNode == this && !haveConnectedEdges && canResetPort(edge.inputPort)) {
            if (edge.inputPort != null) {
                edge.inputPort.resetToDefault();
            }
        }

        updateAllPorts();

        if (onAfterEdgeDisconnected != null) {
            onAfterEdgeDisconnected.accept(edge);
        }
    }

    public void onProcess() {
        inputPorts.PullDatas();
        process();
        if (onProcessed != null) {
            onProcessed.run();
        }
        outputPorts.PushDatas();
    }

    /**
     * Reset the node to its default state
     */
    public void resetNode() {
    }

    /**
     * called by the BaseGraph when the node is added to the graph
     */
    public void initialize(BaseGraph graph) {
        this.graph = graph;
        if (GUID == null) {
            GUID = graph.newGUID().toString();
        } else {
            this.graph.addGUID(GUID);
        }
        enable();
        InitializePorts();
    }

    /**
     * Use this function to initialize anything related to ports generation in your node
     * This will allow the node creation menu to correctly recognize ports that can be connected between nodes
     */
    public void InitializePorts() {
        for (var entry : nodeFields.entrySet()) {
            var nodeField = entry.getValue();
            if (hasCustomBehavior(nodeField)) {
                updatePortsForField(nodeField.fieldName, false);
            } else {
                // If we don't have a custom behavior on the node, we just have to create a simple port
                var port = new PortData()
                        .displayName(nodeField.name)
                        .portColor(nodeField.color)
                        .acceptMultipleEdges(nodeField.isMultiple)
                        .tooltip(Arrays.stream(nodeField.tooltips).toList());
                addPort(nodeField.input, nodeField.fieldName, port);
            }
        }
    }

    /**
     * Update all ports of the node
     */
    public boolean updateAllPorts() {
        var changed = false;
        for (var entry : nodeFields.entrySet()) {
            changed |= updatePortsForField(entry.getValue().fieldName);
        }
        return changed;
    }

    /**
     * Update all ports of the node without updating the connected ports.
     * <br>
     * Only use this method when you need to update all the nodes ports in your graph.
     */
    public boolean UpdateAllPortsLocal() {
        var changed = false;
        for (var entry : nodeFields.entrySet()) {
            changed |= updatePortsForFieldLocal(entry.getValue().fieldName);
        }
        return changed;
    }

    public boolean updatePortsForFieldLocal(String fieldName) {
        return updatePortsForFieldLocal(fieldName, true);
    }

    /**
     * Update the ports related to one java property field and all connected nodes in the graph
     */
    public boolean updatePortsForFieldLocal(String fieldName, boolean sendPortUpdatedEvent) {
        boolean changed = false;
        if (!nodeFields.containsKey(fieldName))
            return false;

        var fieldInfo = nodeFields.get(fieldName);

        if (!hasCustomBehavior(fieldInfo))
            return false;

        List<String> finalPorts = new ArrayList<>();

        var portCollection = fieldInfo.input ? (NodePortContainer) inputPorts : outputPorts;

        // Gather all fields for this port (before to modify them)
        var nodePorts = portCollection.stream().filter(p -> Objects.equals(p.fieldName, fieldName)).toList();
        // Gather all edges connected to these fields:
        var edges = nodePorts.stream().flatMap(port -> port.getEdges().stream()).toList();

        if (fieldInfo.behavior != null) {
            for (var portData : fieldInfo.behavior.handle(edges)) {
                changed |= addPortData(nodePorts, fieldInfo, finalPorts, fieldName, portData);
            }
        }

        // TODO
        // Remove only the ports that are no more in the list
        if (!nodePorts.isEmpty()) {
            var currentPortsCopy = new ArrayList<>(nodePorts);
            for (var currentPort : currentPortsCopy) {
                // If the current port does not appear in the list of final ports, we remove it
                if (finalPorts.stream().noneMatch(id -> Objects.equals(id, currentPort.portData.identifier))) {
                    removePort(fieldInfo.input, currentPort);
                    changed = true;
                }
            }
        }

        // refresh edges ports
        for (var edge : edges) {
            if (fieldInfo.input) {
                var previousPort = edge.inputPort;
                edge.inputPort = getPort(fieldName, edge.inputPortIdentifier);
                if (previousPort != edge.inputPort) {
                    if (previousPort != null) {
                        previousPort.remove(edge);
                    }
                    if (edge.inputPort != null) {
                        edge.inputPort.add(edge);
                    } else {
                        // If the port is null, we disconnect the edge
                        graph.disconnect(edge.GUID);
                    }
                }
            } else {
                var previousPort = edge.outputPort;
                edge.outputPort = getPort(fieldName, edge.outputPortIdentifier);
                if (previousPort != edge.outputPort) {
                    if (previousPort != null) {
                        previousPort.remove(edge);
                    }
                    if (edge.outputPort != null) {
                        edge.outputPort.add(edge);
                    } else {
                        // If the port is null, we disconnect the edge
                        graph.disconnect(edge.GUID);
                    }
                }
            }
        }

        // Make sure the port order is correct:
        portCollection.sort((p1, p2) -> {
            int p1Index = -1;
            int p2Index = -1;
            for (int i = 0; i < finalPorts.size(); i++) {
                if (Objects.equals(p1.portData.identifier, finalPorts.get(i))) {
                    p1Index = i;
                }
                if (Objects.equals(p2.portData.identifier, finalPorts.get(i))) {
                    p2Index = i;
                }
                if (p1Index != -1 && p2Index != -1)
                    break;
            }

            if (p1Index == -1 || p2Index == -1)
                return 0;
            return Integer.compare(p1Index, p2Index);
        });

        if (sendPortUpdatedEvent && onPortsUpdated != null) {
            onPortsUpdated.accept(fieldName);
        }
        return changed;
    }

    private boolean addPortData(List<NodePort> nodePorts, NodeFieldInformation fieldInfo, List<String> finalPorts, String fieldName, PortData portData) {
        var changed = false;
        var port = nodePorts.stream().filter(n -> Objects.equals(n.portData.identifier, portData.identifier)).findFirst().orElse(null);
        // Guard using the port identifier so we don't duplicate identifiers
        if (port == null) {
            addPort(fieldInfo.input, fieldName, portData);
            changed = true;
        } else {
            // in case the port type have changed for an incompatible type, we disconnect all the edges attached to this port
            if (!BaseGraph.areTypesConnectable(port.portData.displayType, portData.displayType)) {
                var copiedEdges = new ArrayList<>(port.getEdges());
                for(var edge : copiedEdges) graph.disconnect(edge.GUID);
            }

            // patch the port data
            if (port.portData != portData) {
                port.portData.CopyFrom(portData);
                changed = true;
            }
        }

        finalPorts.add(portData.identifier);
        return changed;
    }

    protected boolean hasCustomBehavior(NodeFieldInformation info) {
        return info.behavior != null;
    }

    public boolean updatePortsForField(String fieldName) {
        return updatePortsForField(fieldName, true);
    }

    /**
     * Update the ports related to one java property field and all connected nodes in the graph
     */
    public boolean updatePortsForField(String fieldName, boolean sendPortUpdatedEvent) {
        var changed = false;

        fieldsToUpdate.clear();
        updatedFields.clear();

        fieldsToUpdate.push(new PortUpdate(List.of(fieldName), this));

        // Iterate through all the ports that needs to be updated, following graph connection when the
        // port is updated. This is required ton have type propagation multiple nodes that changes port types
        // are connected to each other (i.e. the relay node)
        while (!fieldsToUpdate.isEmpty()) {
            var portUpdate = fieldsToUpdate.pop();
            var fields = portUpdate.fieldNames;
            var node = portUpdate.node;

            // Avoid updating twice a port
            if (updatedFields.contains(portUpdate)) continue;

            updatedFields.add(new PortUpdate(new ArrayList<>(fields), node));

            for (var field : fields) {
                if (node.updatePortsForFieldLocal(field, sendPortUpdatedEvent)) {
                    for (var port : node.isFieldInput(field) ? (NodePortContainer) node.inputPorts : node.outputPorts) {
                        if (!Objects.equals(port.fieldName, field))
                            continue;

                        for (var edge : port.getEdges()) {
                            var edgeNode = (node.isFieldInput(field)) ? edge.outputNode : edge.inputNode;
                            var fieldsWithBehavior = edgeNode.nodeFields.values().stream()
                                    .filter(this::hasCustomBehavior).map(f -> f.fieldName).toList();
                            fieldsToUpdate.push(new PortUpdate(fieldsWithBehavior, edgeNode));
                        }
                    }
                    changed = true;
                }
            }
        }

        return changed;
    }

    /**
     * Add a port
     */
    public void addPort(boolean input, String fieldName, PortData portData) {
        // Fixup port data info if needed:
        if (portData.displayType == null)
            portData.displayType = nodeFields.get(fieldName).info.getType();

        if (input) {
            try {
                inputPorts.add(new NodePort(this, fieldName, portData));
            } catch (NoSuchFieldException e) {
                LDLib2.LOGGER.error("Error while adding input port field:{}, data:{}", fieldName, portData, e);
            }
        }
        else {
            try {
                outputPorts.add(new NodePort(this, fieldName, portData));
            } catch (NoSuchFieldException e) {
                LDLib2.LOGGER.error("Error while adding output port field:{}, data:{}", fieldName, portData, e);
            }
        }
    }

    /**
     * Remove a port
     */
    public void removePort(boolean input, NodePort port) {
        if (input)
            inputPorts.remove(port);
        else
            outputPorts.remove(port);
    }

    /**
     * Remove port(s) from field name
     */
    public void removePort(boolean input, String fieldName) {
        if (input)
            inputPorts.removeIf(p -> Objects.equals(p.fieldName, fieldName));
        else
            outputPorts.removeIf(p -> Objects.equals(p.fieldName, fieldName));
    }

    /**
     * Get all the nodes connected to the input ports of this node
     */
    public List<BaseNode> getInputNodes() {
        return inputPorts.stream().flatMap(p -> p.getEdges().stream()).map(e -> e.outputNode).toList();
    }

    /**
     * Get all the nodes connected to the output ports of this node
     */
    public List<BaseNode> GetOutputNodes() {
        return outputPorts.stream().flatMap(p -> p.getEdges().stream()).map(e -> e.inputNode).toList();
    }

    /**
     * Return a node matching the condition in the dependencies of the node
     */
    public BaseNode findInDependencies(Predicate<BaseNode> condition) {
        Stack<BaseNode> dependencies = new Stack<>();
        dependencies.push(this);
        var depth = 0;
        while (!dependencies.isEmpty()) {
            var node = dependencies.pop();

            // Guard for infinite loop (faster than a HashSet based solution)
            depth++;
            if (depth > 2000)
                break;

            if (condition.test(node))
                return node;

            for (var dep : node.getInputNodes())
                dependencies.push(dep);
        }
        return null;
    }

    /**
     * Get the port from field name and identifier
     */
    @Nullable
    public NodePort	getPort(String fieldName, String identifier) {
        var ports = new ArrayList<>(inputPorts);
        ports.addAll(outputPorts);
        return ports.stream().filter(p -> {
            var bothNull = (identifier == null || identifier.isEmpty()) && (p.portData.identifier == null || p.portData.identifier.isEmpty());
            return Objects.equals(p.fieldName, fieldName) && (bothNull || Objects.equals(identifier, p.portData.identifier));
        }).findFirst().orElse(null);
    }

    @Nullable
    public NodePort	getPort(String fieldName) {
        return getPort(fieldName, null);
    }

    /**
     * Return all the ports of the node
     */
    public List<NodePort> getAllPorts() {
        var ports = new ArrayList<>(inputPorts);
        ports.addAll(outputPorts);
        return ports;
    }

    /**
     * Return all the connected edges of the node
     */
    public List<PortEdge> getAllEdges() {
        var edges = new ArrayList<PortEdge>();
        for (var port : getAllPorts()) {
            edges.addAll(port.getEdges());
        }
        return edges;
    }

    /**
     * Is the field an input field
     */
    public boolean isFieldInput(String fieldName){
        return nodeFields.get(fieldName).input;
    }


    /**
     * Called when the node is enabled
     */
    protected void enable() {
    }

    /**
     * Called when the node is disabled
     */
    protected void disable() {
    }

    /**
     * Called when the node is removed
     */
    protected void destroy() {
    }

    /**
     * Override this method to implement custom processing
     */
    protected void process() {
    }

    public static class NodeFieldInformation {
        public String name;
        public String fieldName;
        public Field info;
        public boolean input;
        public int color;
        public boolean isMultiple;
        public String[] tooltips;
        @Nullable
        public ICustomPortBehaviorDelegate behavior;
        public int priority;

        public NodeFieldInformation(Field info, String name, int color, boolean input, boolean isMultiple, String[] tooltips,
                                    int priority, @Nullable ICustomPortBehaviorDelegate behavior) {
            this.input = input;
            this.color = color;
            this.isMultiple = isMultiple;
            this.info = info;
            this.name = name;
            this.fieldName = info.getName();
            this.behavior = behavior;
            this.tooltips = tooltips;
            this.priority = priority;
        }
    }
}
