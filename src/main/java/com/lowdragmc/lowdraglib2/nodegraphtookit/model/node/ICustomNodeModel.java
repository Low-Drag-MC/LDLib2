package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.INodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;

import java.util.List;
import java.util.Map;

public interface ICustomNodeModel {
    /**
     * Initializes the provided {@link Node} instance with custom settings or configurations.
     *
     * <p>This method is used to prepare a {@link Node} object, allowing it to be customized
     * or configured for use within a graph-based workflow. This may involve setting up its
     * options, connections, or other properties as required.</p>
     *
     * @param node the {@link Node} instance to initialize
     */
    void initCustomNode(Node node);

    /**
     * Retrieves the {@link Node} associated with this model.
     *
     * @return the {@link Node} instance that represents the current object in a graph-based context.
     */
    Node getNode();

    /**
     * Retrieves a mapping of option identifiers to their respective {@link INodeOption} instances.
     *
     * <p>The key in the returned map represents the unique identifier of the option,
     * and the value is the corresponding {@link INodeOption} object containing details about the option.
     *
     * @return a {@code Map<String, INodeOption>} where each entry associates
     *         an option ID with its corresponding {@link INodeOption}.
     */
    Map<String, INodeOption> getOptionsById();

    /**
     * Retrieves a list of {@link NodeOption} objects associated with the current node model.
     *
     * The returned list contains instances of {@link NodeOption}, each representing a configurable option
     * associated with the node. These options define custom attributes or functionalities that can be
     * applied to the node.
     *
     * @return a {@code List<NodeOption>} containing the configurable options associated with the node.
     */
    List<NodeOption> getNodeOptions();

    /**
     * Retrieves the {@link INodeOption} associated with the specified identifier.
     *
     * The method uses the provided {@code id} to look up the corresponding {@link INodeOption}
     * in the mapping returned by {@link #getOptionsById()}.
     *
     * @param id the unique identifier of the {@link INodeOption} to retrieve
     * @return the {@link INodeOption} associated with the given {@code id},
     *         or {@code null} if no matching option is found
     */
    default INodeOption getNodeOption(String id) {
        return getOptionsById().get(id);
    }
}
