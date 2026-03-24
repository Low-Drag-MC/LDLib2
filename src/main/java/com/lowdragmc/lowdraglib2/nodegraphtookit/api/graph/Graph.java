package com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.INode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.IVariable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents the core definition of a graph and defines its behavior.
 */
public abstract class Graph implements IGraph {
    /** Backing implementation that stores the actual graph state. */
    public final CustomGraphModelImpl graphModel = createGraphModel();

    protected CustomGraphModelImpl createGraphModel() {
        return new CustomGraphModelImpl(this);
    }

    /**
     * Retrieves a list of supported node types in the graph.
     * <p>
     * Typically backed by an {@link com.lowdragmc.lowdraglib2.registry.AutoRegistry} created via
     * {@link com.lowdragmc.lowdraglib2.registry.AutoRegistry#createNodeRegistry}.
     *
     * @return a {@link List} of {@code Class} objects representing the supported node types
     */
    public abstract List<Class<? extends Node>> getSupportNodes();

    /**
     * Retrieves a list of supported types for the graph.
     *
     * @return a {@link List} of {@link TypeHandle} objects representing the supported types,
     * or {@code null} if no specific types are explicitly supported, it will be automatically detected by nodes ports.
     */
    public @Nullable List<TypeHandle> getSupportTypes() {
        return null;
    }

    /**
     * Retrieves a variable declared in the graph by index.
     *
     * <p>Use this method to access a specific {@link IVariable} from the list of variables declared in the graph.
     * This list does not include variable nodes that reference variables.
     * The index is zero-based and reflects the order in which the variables were created.
     *
     * @param index the index of the variable to retrieve (zero-based)
     * @return the {@link IVariable} at the specified index
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public IVariable getVariable(int index) {
        return graphModel.getVariableModels().get(index);
    }

    /**
     * Retrieves all variables declared in the graph.
     *
     * <p>Use this method to enumerate all {@link IVariable}s declared in the graph.
     * This list does not include variable nodes that reference variables.
     * The collection reflects the variables as declared, in their order of creation.
     *
     * @return an {@link Iterable} of all declared {@link IVariable}s
     */
    public List<? extends IVariable> getVariables() {
        return graphModel.getVariableModels();
    }

    /**
     * Retrieves a node defined in the graph by its index.
     *
     * <p>Use this method to access a node based on its creation order in the graph.
     *
     * <p>The list includes:
     * <ul>
     *   <li>Your own {@code Node}s</li>
     *   <li>{@code ContextNode}s</li>
     *   <li>{@code IVariableNode}s</li>
     *   <li>{@code IConstantNode}s</li>
     *   <li>{@code ISubgraphNode}s</li>
     * </ul>
     * It excludes {@code BlockNode}s, which are only accessible through their parent {@code ContextNode}.
     *
     * @param index the zero-based index of the node to retrieve
     * @return the {@link INode} at the specified index
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public INode getNode(int index) {
        return graphModel.getNodes().get(index);
    }

    /**
     * Retrieves all nodes in the graph.
     *
     * <p>Use this method to access every node in the graph. Nodes are returned in the order they were created.
     *
     * <p>The list includes:
     * <ul>
     *   <li>Your own {@code Node}s</li>
     *   <li>{@code ContextNode}s</li>
     *   <li>{@code IVariableNode}s</li>
     *   <li>{@code IConstantNode}s</li>
     *   <li>{@code ISubgraphNode}s</li>
     * </ul>
     * It excludes {@code BlockNode}s, which are only accessible through their parent {@code ContextNode}.
     *
     * @return an {@link Iterable} of all {@link INode}s in the graph
     */
    public List<? extends INode> getNodes() {
        return graphModel.getNodes();
    }
}
