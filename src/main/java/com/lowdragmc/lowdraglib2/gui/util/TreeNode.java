package com.lowdragmc.lowdraglib2.gui.util;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/***
 * Tree
 * @param <T> key
 * @param <K> leaf
 */
public class TreeNode<T, K> {
    public final int dimension;
    protected final T key;
    @Nullable
    protected K content;
    @Nullable
    protected List<TreeNode<T, K>> children;
    @Nullable
    protected Predicate<TreeNode<T, K>> valid;

    public TreeNode(int dimension, T key) {
        this.dimension = dimension;
        this.key = key;
    }

    public TreeNode<T, K> setValid(Predicate<TreeNode<T, K>> valid) {
        this.valid = valid;
        return this;
    }

    public boolean isLeaf(){
        return children == null || getChildren().isEmpty();
    }

    public boolean isBranch(){
        return !isLeaf();
    }

    public TreeNode<T, K> getOrCreateChild (T childKey) {
        TreeNode<T, K> result;
        if (children != null) {
            result = getChildren().stream().filter(child->child.key.equals(childKey)).findFirst().orElseGet(()->{
                TreeNode<T, K> newNode = new TreeNode<T, K>(dimension + 1, childKey).setValid(valid);
                getChildren().add(newNode);
                return newNode;
            });
        } else {
            children = new ArrayList<>();
            result = new TreeNode<T, K>(dimension + 1, childKey).setValid(valid);
            getChildren().add(result);
        }
        return result;
    }

    public TreeNode<T, K> createChild (T childKey) {
        if (children == null) {
            children = new ArrayList<>();
        }
        TreeNode<T, K> result = new TreeNode<T, K>(dimension + 1, childKey).setValid(valid);
        getChildren().add(result);
        return result;
    }

    public TreeNode<T, K> getChild(T key) {
        if (getChildren() != null) {
            for (TreeNode<T, K> child : getChildren()) {
                if (child.key.equals(key)) {
                    return child;
                }
            }
        }
        return null;
    }

    public void addContent(T key, K content) {
        getOrCreateChild(key).content = content;
    }

    public T getKey() {
        return key;
    }

    @Nullable
    public K getContent() {
        return content;
    }

    public List<TreeNode<T, K>> getChildren() {
        if (children == null) return Collections.emptyList();
        if (valid == null) return children;
        return children.stream().filter(valid).collect(Collectors.toList());
    }

    public void removeChild(T key) {
        if (children != null) {
            for (TreeNode<T, K> child : children) {
                if (child.key.equals(key)) {
                    children.remove(child);
                    return;
                }
            }
        }
    }

    public void removeChild(TreeNode<T, K> child) {
        if (children != null) {
            children.remove(child);
        }
    }

    @Override
    public String toString() {
        return key.toString();
    }
}
