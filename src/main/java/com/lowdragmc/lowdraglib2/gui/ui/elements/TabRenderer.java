package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "tab", registry = "ldlib2:ui_element_renderer")
public final class TabRenderer extends DelegatingUIElementRenderer<Tab, TabRenderer> {
    @Override
    public Class<Tab> type() {
        return Tab.class;
    }

    @Override
    public void drawBackgroundAdditional(Tab tab, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(tab, context);
            return;
        }
        drawBackgroundAdditional(tab, guiContext);
    }

    static void drawBackgroundAdditional(Tab tab, GUIContext context) {
        var texture = tab.getTabStyle().baseTexture();
        if (tab.isSelected()) {
            texture = tab.getTabStyle().selectedTexture();
        } else if (tab.isHovered()) {
            texture = tab.getTabStyle().hoverTexture();
        }
        context.drawTexture(texture, tab.getPositionX(), tab.getPositionY(), tab.getSizeWidth(), tab.getSizeHeight());
    }
}
