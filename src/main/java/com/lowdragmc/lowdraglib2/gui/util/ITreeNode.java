package com.lowdragmc.lowdraglib2.gui.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface ITreeNode<KEY, CONTENT> {
    /**
     * Determines the spatial dimension of the tree node. The root node is 0.
     */
    int getDimension();

    /**
     * Retrieves the unique key associated with the tree node.
     *
     * @return the key of the tree node, guaranteed to be non-null.
     */
    @Nonnull KEY getKey();

    /**
     * Retrieves the content associated with the tree node.
     *
     * This method can return null if the node does not have any content associated with it.
     *
     * @return the content of the tree node, or null if no content is available.
     */
    @Nullable CONTENT getContent();

    /**
     * Checks if the current node is a leaf node in the tree structure.
     * A leaf node is defined as a node that does not have any child nodes.
     *
     * @return {@code true} if the node is a leaf (has no children), {@code false} otherwise.
     */
    default boolean isLeaf() {
        return getChildren().isEmpty();
    }

    /**
     * Checks if the current node is a branch node in the tree structure.
     * A branch node is defined as a node that is not a leaf, meaning it has at least one child node.
     *
     * @return {@code true} if the node is a branch (has child nodes), {@code false} otherwise.
     */
    default boolean isBranch() {
        return !isLeaf();
    }

    /**
     * Retrieves the list of child nodes of the current tree node.
     *
     * The returned list contains all the immediate child nodes of this node.
     * If the node does not have any children, an empty list is returned.
     * This method guarantees that the returned list is non-null.
     *
     * @return a non-null list of child nodes, potentially empty.
     */
    @Nonnull
    List<? extends ITreeNode<KEY, CONTENT>> getChildren();

    /**
     * Retrieves a child node of the current node that matches the specified key.
     * Iterates through the list of child nodes and returns the first node whose
     * key equals the provided key.
     *
     * @param key the key to search for among the child nodes, must not be null.
     * @return the child node with the matching key, or null if no such child exists.
     */
    @Nullable
    default ITreeNode<KEY, CONTENT> getChild(KEY key) {
        for (ITreeNode<KEY, CONTENT> child : getChildren()) {
            if (child.getKey().equals(key)) {
                return child;
            }
        }
        return null;
    }

}
