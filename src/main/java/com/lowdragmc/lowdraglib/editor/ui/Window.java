package com.lowdragmc.lowdraglib.editor.ui;

import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;

public class Window extends UIElement {
    public final TabView tabView;

    public Window() {
        this.tabView = new TabView();
        this.tabView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlexGrow(1);
        }).setId("tab_view");
        getStyle().backgroundTexture(Sprites.BORDER);
    }
}
