package com.lowdragmc.lowdraglib.editor.ui;

import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.elements.Tab;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

public class View extends UIElement {
    @Getter @Setter
    private String name;

    public View() {}

    public View(String name) {
        this.name = name;
    }

    /**
     * Get the name of the view.
     */
    protected Component getViewName() {
        return Component.translatable(name);
    }

    /**
     * Create a tab for this view which will be displayed in the window's tab view.
     */
    public Tab craeteTab() {
        return new Tab().setText(getViewName());
    }

}
