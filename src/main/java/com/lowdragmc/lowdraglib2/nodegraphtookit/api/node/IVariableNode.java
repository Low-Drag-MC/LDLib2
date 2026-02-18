package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.IVariable;

/**
 * Interface for a variable node, which is a specialized node that references an {@link IVariable} defined in the graph.
 *
 * <p>Variable nodes represent a reference to a declared {@link IVariable} in the graph.
 * They are distinct from {@link IVariable}s, which are declarations displayed as capsules in the graph's Blackboard.
 * You can drag and drop an {@link IVariable} from the Blackboard into the graph canvas to create a variable node.
 * The variable node is an instance of the declared {@link IVariable} and appears in the graph.</p>
 */
public interface IVariableNode extends INode {

    /**
     * Retrieves the {@link IVariable} associated with this node.
     *
     * <p>This property returns the variable that this node references. The variable defines the node's data type
     * and determines the port behavior. The returned variable is declared in the graph's Blackboard and shared
     * across all variable nodes that reference it.</p>
     *
     * @return the variable referenced by this node
     */
    IVariable getVariable();
}
