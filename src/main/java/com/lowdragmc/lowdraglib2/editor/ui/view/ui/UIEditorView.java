package com.lowdragmc.lowdraglib2.editor.ui.view.ui;

import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;

import javax.annotation.Nullable;

public class UIEditorView extends View {
    public final UIHierarchy hierarchy;
    public final GraphView graphView = new GraphView();
    public final Inspector inspector = new Inspector();

    public UIEditorView() {
        super("editor.view.ui_editor");
        this.hierarchy = new UIHierarchy(this);
        getLayout().setFlexDirection(YogaFlexDirection.ROW);

        hierarchy.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        });

        graphView.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        });

        inspector.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        });
        inspector.scrollerView.viewPort.layout(layout -> {
            layout.setPadding(YogaEdge.ALL, 5);
        }).style(style -> style.backgroundTexture(Sprites.BORDER));

        addChildren(new SplitView.Horizontal().setPercentage(20)
                .left(hierarchy)
                .right(new SplitView.Horizontal().setPercentage(64)
                        .left(graphView)
                        .right(inspector)));
    }

    public UIEditorView loadUI(UI ui) {
        this.graphView.clearAllContentChildren();
        this.hierarchy.loadUI(ui);
        this.graphView.addContentChild(ui.getRootElement());
        return this;
    }

    public <T, C> Menu<T, C> openMenu(float posX, float posY, TreeNode<T, C> menuNode, UIElementProvider<T> uiProvider) {
        var menu = new Menu<>(menuNode, uiProvider);
        menu.layout(layout -> {
            layout.setPosition(YogaEdge.LEFT, posX - getContentX());
            layout.setPosition(YogaEdge.TOP, posY - getContentY());
        });
        addChildren(menu);
        return menu;
    }

    public void openMenu(float posX, float posY, @Nullable TreeBuilder.Menu menuBuilder) {
        if (menuBuilder == null || menuBuilder.isEmpty()) return;
        openMenu(posX, posY, menuBuilder.build(), TreeBuilder.Menu::uiProvider)
                .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                .setOnNodeClicked(TreeBuilder.Menu::handle);
    }
}
