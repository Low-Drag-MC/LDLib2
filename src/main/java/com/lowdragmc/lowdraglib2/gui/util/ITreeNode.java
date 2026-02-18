package com.lowdragmc.lowdraglib2.gui.util;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
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
     * Retrieves the parent node of the current tree node.
     * The parent node is the node immediately above the current node in the hierarchy.
     * If the current node is a root node (i.e., it has no parent), this method will return {@code null}.
     *
     * @return the parent node of the current node, or {@code null} if the node is a root.
     */
    @Nullable
    ITreeNode<KEY, CONTENT> getParent();
    
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
     * Flattens the tree structure starting from the current node into a single list.
     *
     * The resulting list contains the current node followed by all its descendants
     * in a depth-first traversal order.
     *
     * The method recursively retrieves the child nodes from each node
     * using {@link #getChildren()} and aggregates them into a single list.
     *
     * @return a list of nodes starting with the current node and including
     *         all its descendants in depth-first order. The resulting list is non-null
     *         and may be empty if the current node is a leaf.
     */
    default List<? extends ITreeNode<KEY, CONTENT>> flatten() {
        var result = new ArrayList<ITreeNode<KEY, CONTENT>>();
        result.add(this);
        for (var child : getChildren()) {
            result.addAll(child.flatten());
        }
        return result;
    }

    /**
     * Determines the index of the current node among its siblings in the parent's child list.
     * <p>
     * The sibling index is calculated based on the position of this node in the list of children
     * returned by {@link #getParent()}{@code .getChildren()}. If the current node does not have a
     * parent (i.e., it is a root node), the method will return {@code -1}.
     *
     * @return the zero-based index of the node within its sibling list, or {@code -1} if the node
     * does not have a parent.
     */
    default int getSiblingIndex() {
        if (getParent() == null) return -1;
        return getParent().getChildren().indexOf(this);
    }

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
