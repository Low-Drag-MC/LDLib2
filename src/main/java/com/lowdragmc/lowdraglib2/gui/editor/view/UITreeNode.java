package com.lowdragmc.lowdraglib2.gui.editor.view;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

public class UITreeNode implements ITreeNode<UIElement, Void> {
    @Getter
    @Nullable
    public final UITreeNode parent;
    @Getter
    public final int dimension;
    @Getter
    public final UIElement key;

    public UITreeNode(UIElement root) {
        this(null, 0, root);
    }

    private UITreeNode(@Nullable UITreeNode parent, int dimension, UIElement node) {
        this.parent = parent;
        this.dimension = dimension;
        this.key = node;
    }

    @Override
    public @Nullable Void getContent() {
        return null;
    }

    @Override
    @Nonnull
    public List<UITreeNode> getChildren() {
        return key.getEditorVisibleChildren().stream().map(child -> new UITreeNode(this, dimension + 1, child)).toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UITreeNode that = (UITreeNode) o;
        return dimension == that.dimension &&
                java.util.Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(dimension, key);
    }
}
