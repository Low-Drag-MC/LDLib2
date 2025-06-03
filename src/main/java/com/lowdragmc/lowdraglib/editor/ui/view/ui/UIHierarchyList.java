package com.lowdragmc.lowdraglib.editor.ui.view.ui;

import com.lowdragmc.lowdraglib.gui.ui.UI;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.util.TreeNode;

public class UIHierarchyList extends UIElement {
    public final UIEditorView editorView;
    public final UI ui;
    public final ScrollerView scrollerView = new ScrollerView();
    public final TreeNode<UIElement, UIElement> rootNode;

    public UIHierarchyList(UIEditorView editorView) {
        this.editorView = editorView;
        this.ui = editorView.ui;
        this.rootNode = buildRootNode();
        scrollerView.layout(layout -> {
           layout.setWidthPercent(100);
           layout.setHeightPercent(100);
        });

        scrollerView.addScrollViewChild(new TreeList<>(rootNode).setCanSelectNode(true));
        this.addChild(scrollerView);
    }

    protected TreeNode<UIElement, UIElement> buildRootNode() {
        var builder = new TreeBuilder<UIElement, UIElement>(ui.rootElement);
        buildDFS(builder, ui.rootElement);
        return builder.build();
    }

    private void buildDFS(TreeBuilder<UIElement, UIElement> builder, UIElement current) {
        builder.content(current);
        for (UIElement child : current.getChildren()) {
            builder.branch(child, b -> buildDFS(b, child));
        }
    }

}
