package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.INodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;

import java.util.List;

/**
 * Interface for node model implementations that support user-defined nodes.
 *
 * <p>This interface is implemented by node models that back user-defined {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node} instances.</p>
 */
public interface INodeWithOptions {

    /**
     * Gets the list of node options defined on this node.
     *
     * @return the list of node options
     */
    List<? extends INodeOption> getNodeOptions();

    /**
     * Gets a node option by its unique name.
     *
     * @param name the unique name of the option
     * @return the node option, or {@code null} if not found
     */
    INodeOption getNodeOptionById(String name);

    default int getOptionCount() {
        return getNodeOptions().size();
    }
}
