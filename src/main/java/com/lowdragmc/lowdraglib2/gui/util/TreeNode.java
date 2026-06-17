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

/***
 * Tree
 * @param <T> key
 * @param <K> leaf
 */
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

    protected TreeNode<T, K> createNode(@Nullable TreeNode<T, K> parent, int dimension, T key) {
        return new TreeNode<>(parent, dimension, key);
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
            result = children.stream().filter(child -> areKeysEqual(child.key, childKey)).findFirst().orElseGet(()->{
                TreeNode<T, K> newNode = createNode(this, dimension + 1, childKey).setValid(valid);
                children.add(newNode);
                return newNode;
            });
        } else {
            children = new ArrayList<>();
            result = createNode(this,  dimension + 1, childKey).setValid(valid);
            children.add(result);
        }
        return result;
    }

    public TreeNode<T, K> createChild (T childKey) {
        if (children == null) {
            children = new ArrayList<>();
        }
        TreeNode<T, K> result = createNode(this, dimension + 1, childKey).setValid(valid);
        children.add(result);
        return result;
    }

    public void addContent(T key, K content) {
        getOrCreateChild(key).content = content;
    }

    public void removeChild(T key) {
        if (children != null) {
            for (TreeNode<T, K> child : children) {
                if (areKeysEqual(child.key, key)) {
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

    public boolean areKeysEqual(T key1, T key2) {
        return Objects.equals(key1, key2);
    }

    public boolean areContentsEqual(K content1, K content2) {
        return Objects.equals(content1, content2);
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
                areKeysEqual(key, (T) that.key) &&
                areContentsEqual(content, (K) that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, key, content);
    }
}
