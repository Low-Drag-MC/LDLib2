package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.network.chat.Component;

public interface IGraphTool {
    default UIElement getUIElement() {
        return (UIElement) this;
    }

    Component getTitle();
}
