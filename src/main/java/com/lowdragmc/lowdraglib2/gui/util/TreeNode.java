package com.lowdragmc.lowdraglib2.gui.util;

import lombok.Getter;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
public class TreeNode<T, K> implements ITreeNode<T, K> {
    @Nullable
    @Getter
    public final TreeNode<T, K> parent;
    @Getter
    public final int dimension;
    @Getter
    protected final T key;
    @Nullable
    @Getter
    protected K content;
    @Nullable
    protected List<TreeNode<T, K>> children;
    @Nullable
    protected Predicate<TreeNode<T, K>> valid;

    public TreeNode(T key) {
        this(null, 0, key);
    }

    public TreeNode(@Nullable TreeNode<T, K> parent, int dimension, T key) {
        this.parent = parent;
        this.dimension = dimension;
        this.key = key;
    }

    @Nonnull
    public List<? extends TreeNode<T, K>> getChildren() {
        if (children == null) return Collections.emptyList();
        if (valid == null) return children;
        return children.stream().filter(valid).collect(Collectors.toList());
    }

    public TreeNode<T, K> setValid(Predicate<TreeNode<T, K>> valid) {
        this.valid = valid;
        return this;
    }

    public TreeNode<T, K> getOrCreateChild(T childKey) {
        TreeNode<T, K> result;
        if (children != null) {
            result = children.stream().filter(child->child.key.equals(childKey)).findFirst().orElseGet(()->{
                TreeNode<T, K> newNode = new TreeNode<>(this, dimension + 1, childKey).setValid(valid);
                children.add(newNode);
                return newNode;
            });
        } else {
            children = new ArrayList<>();
            result = new TreeNode<>(this,  dimension + 1, childKey).setValid(valid);
            children.add(result);
        }
        return result;
    }

    public TreeNode<T, K> createChild (T childKey) {
        if (children == null) {
            children = new ArrayList<>();
        }
        TreeNode<T, K> result = new TreeNode<>(this, dimension + 1, childKey).setValid(valid);
        children.add(result);
        return result;
    }

    public void addContent(T key, K content) {
        getOrCreateChild(key).content = content;
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeNode<?, ?> that = (TreeNode<?, ?>) o;
        return dimension == that.dimension &&
                Objects.equals(key, that.key) &&
                Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, key, content);
    }
}
