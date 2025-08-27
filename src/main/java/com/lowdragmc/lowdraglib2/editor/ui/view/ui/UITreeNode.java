package com.lowdragmc.lowdraglib2.editor.ui.view.ui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

@EqualsAndHashCode
public class UITreeNode implements ITreeNode<UIElement, Void> {
    @Getter
    public final int dimension;
    @Getter
    public final UIElement key;

    public UITreeNode(UIElement root) {
        this(0, root);
    }

    private UITreeNode(int dimension, UIElement node) {
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
        return key.getEditorVisibleChildren().stream().map(child -> new UITreeNode(dimension + 1, child)).toList();
    }
}
