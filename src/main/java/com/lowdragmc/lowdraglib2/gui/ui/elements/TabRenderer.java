package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TabRenderer {
    private TabRenderer() {
    }

    public static void drawBackgroundAdditional(Tab tab, GUIContext context) {
        var texture = tab.getTabStyle().baseTexture();
        if (tab.isSelected()) {
            texture = tab.getTabStyle().selectedTexture();
        } else if (tab.isHovered()) {
            texture = tab.getTabStyle().hoverTexture();
        }
        context.drawTexture(texture, tab.getPositionX(), tab.getPositionY(), tab.getSizeWidth(), tab.getSizeHeight());
    }
}
